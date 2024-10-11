package com.rockthejvm.reviewboard.integration

import com.rockthejvm.reviewboard.config.{JWTConfig, RecoveryTokensConfig}
import com.rockthejvm.reviewboard.domain.data.UserToken
import com.rockthejvm.reviewboard.http.controllers.UserController
import com.rockthejvm.reviewboard.http.requests.{
  DeleteAccountRequest,
  ForgotPasswordRequest,
  LoginRequest,
  RecoverPasswordRequest,
  RegisterUserAccount,
  UpdatePasswordRequest
}
import com.rockthejvm.reviewboard.http.responses.UserResponse
import com.rockthejvm.reviewboard.repositories.{
  RecoveryTokensRepositoryLive,
  Repository,
  RepositorySpec,
  UserRepository,
  UserRepositoryLive
}
import com.rockthejvm.reviewboard.services.{
  EmailService,
  EmailServiceLive,
  JWTServiceLive,
  UserServiceLive
}
import sttp.client3.{basicRequest, SttpBackend, UriContext}
import sttp.client3.testing.SttpBackendStub
import sttp.model.Method
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*
import zio.test.TestAspect.ignore

object UserFlowSpec extends ZIOSpecDefault with RepositorySpec {

  // http controller
  // service
  // repository
  // test container

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  override val initScript: String = "sql/integration.sql"

  private def backendSubZIO =
    for {
      // create the controller
      controller <- UserController.makeZIO
      // build tapir backend
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(zioME))
          .whenServerEndpointsRunLogic(controller.routes)
          .backend()
      )
    } yield backendStub

  extension [A: JsonCodec](backend: SttpBackend[Task, Nothing]) {
    def sendRequest[B: JsonCodec](
        method: Method,
        path: String,
        payload: A,
        maybeToken: Option[String] = None
    ): Task[Option[B]] =
      basicRequest
        .method(method, uri"$path")
        .body(
          payload.toJson
        )
        .auth
        .bearer(maybeToken.getOrElse(""))
        .send(backend)
        .map(_.body)
        .map(_.toOption.flatMap(payload => payload.fromJson[B].toOption))

    def post[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.POST, path, payload)

    def postNoResponseBody(path: String, payload: A): Task[Unit] = {
      basicRequest
        .method(Method.POST, uri"$path")
        .body(payload.toJson)
        .send(backend)
        .unit
    }

    def postAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.POST, path, payload, Some(token))

    def put[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload)

    def putAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.PUT, path, payload, Some(token))

    def delete[B: JsonCodec](path: String, payload: A): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload)

    def deleteAuth[B: JsonCodec](path: String, payload: A, token: String): Task[Option[B]] =
      sendRequest(Method.DELETE, path, payload, Some(token))
  }

  class EmailServiceProbe extends EmailService {
    val db = collection.mutable.Map[String, String]()

    override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit

    override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] =
      ZIO.succeed {
        db += (to -> token)
      }

    // specific to the test
    def probeToken(email: String): Task[Option[String]] = ZIO.succeed(db.get(email))
  }

  val emailServiceLayer = ZLayer.succeed(
    new EmailServiceProbe
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("UserSpecFlow")(
      test("create user") {
        for {
          backendStub <- backendSubZIO
          response <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("daniel@rockthejvm.com", "rockthejvm")
          )
          _ <- ZIO.succeed(println(s"User-2:$response"))
        } yield assertTrue(
          response.contains(UserResponse("daniel@rockthejvm.com"))
        )
      },
      test("create user and log in") {
        for {
          backendStub <- backendSubZIO
          response <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("daniel@rockthejvm.com", "rockthejvm")
          )
          _ <- ZIO.succeed(println(s"User-1:$response"))
          maybeToken <- backendStub.post[UserToken](
            "/users/login",
            LoginRequest("daniel@rockthejvm.com", "rockthejvm")
          )
        } yield assertTrue(
          maybeToken.filter(_.email == "daniel@rockthejvm.com").nonEmpty
        )
      },
      test("change password") {
        for {
          backendStub <- backendSubZIO
          response <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("daniel@rockthejvm.com", "rockthejvm")
          )

          userToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "rockthejvm"))
            .someOrFail(new RuntimeException("Authentication failed"))

          _ <- backendStub
            .putAuth[UserResponse](
              "/users/password",
              UpdatePasswordRequest("daniel@rockthejvm.com", "rockthejvm", "scalarules"),
              userToken.token
            )

          maybeOldToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "rockthejvm"))

          maybeNewToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "scalarules"))

        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      },
      test("delete user") {
        for {
          backendStub <- backendSubZIO
          userRepo    <- ZIO.service[UserRepository]

          response <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount("daniel@rockthejvm.com", "rockthejvm")
          )

          maybeOldUser <- userRepo.getByEmail("daniel@rockthejvm.com")

          userToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "rockthejvm"))
            .someOrFail(new RuntimeException("Authentication failed"))

          _ <- backendStub
            .deleteAuth[UserResponse](
              "/users",
              DeleteAccountRequest("daniel@rockthejvm.com", "rockthejvm"),
              userToken.token
            )

          maybeUser <- userRepo.getByEmail("daniel@rockthejvm.com")

        } yield assertTrue(
          maybeOldUser.filter(_.email == "daniel@rockthejvm.com").nonEmpty &&
            maybeUser.filter(_.email == "daniel@rockthejvm.com").isEmpty
        )
      },
      test("recover password flow") {
        val email = "daniel@rockthejvm.com"
        for {
          backendStub <- backendSubZIO
          // register user
          _ <- backendStub.post[UserResponse](
            "/users",
            RegisterUserAccount(email, "rockthejvm")
          )
          // trigger recover password flow
          _ <- backendStub.postNoResponseBody(
            "/users/forgot",
            ForgotPasswordRequest("daniel@rockthejvm.com")
          )
          // fetch the token
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          token <- emailServiceProbe
            .probeToken(email)
            .someOrFail(new RuntimeException("Token was not emailed"))
          _ <- backendStub.postNoResponseBody(
            "/users/recover",
            RecoverPasswordRequest(email, token, "scalarules")
          )
          maybeOldToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "rockthejvm"))

          maybeNewToken <- backendStub
            .post[UserToken]("/users/login", LoginRequest("daniel@rockthejvm.com", "scalarules"))

        } yield assertTrue(
          maybeOldToken.isEmpty && maybeNewToken.nonEmpty
        )
      }
    ).provide(
      Scope.default,
      UserServiceLive.layer,
      JWTServiceLive.layer,
      UserRepositoryLive.layer,
      RecoveryTokensRepositoryLive.layer,
      Repository.quillLayer,
      dataSourceLayer,
      emailServiceLayer,
      ZLayer.succeed(JWTConfig("secret", 3600)),
      ZLayer.succeed(RecoveryTokensConfig(3600))
    )
}

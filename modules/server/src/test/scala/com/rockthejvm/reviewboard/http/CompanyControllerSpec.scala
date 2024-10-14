package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.domain.data.{Company, User, UserId, UserToken}
import com.rockthejvm.reviewboard.http.controllers.CompanyController
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import com.rockthejvm.reviewboard.services.{CompanyService, JWTService}
import sttp.client3.*
import sttp.client3.testing.SttpBackendStub
import sttp.monad.MonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.*
import zio.json.*
import zio.test.*
import com.rockthejvm.reviewboard.syntax.*

object CompanyControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val rtjvm = Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  val daniel = User(
    1L,
    "daniel@rockthejvm.com",
    "1000:C5714C45AC5794A4481F94BB2D6FF76C4605F0FFFC09A07D:823075578976B713EAC4F4BF35ADE8B462DA1B77B7A5550C"
  )

  private val serviceStub = new CompanyService {
    override def create(req: CreateCompanyRequest): Task[Company] = ZIO.succeed(rtjvm)

    override def getAll: Task[List[Company]] = ZIO.succeed(List(rtjvm))

    override def getById(id: Long): Task[Option[Company]] = ZIO.succeed {
      if (id == 1) Some(rtjvm) else None
    }

    override def getBySlug(slug: String): Task[Option[Company]] = ZIO.succeed {
      if (slug == rtjvm.slug) Some(rtjvm) else None
    }
  }

  val stubJwtLayer = ZLayer.succeed {
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "SECRET", 10000))

      override def verifyToken(token: String): Task[UserId] =
        ZIO.succeed(UserId(daniel.id, daniel.email))
    }
  }

  private def backendSubZIO(endpointFun: CompanyController => ServerEndpoint[Any, Task]) =
    for {
      // create the controller
      controller <- CompanyController.makeZIO
      // build tapir backend
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(zioME))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] = suite("Company Controller Spec")(
    test("post company") {
      val program = for {
        backendStub <- backendSubZIO(_.create)
        // run http request
        response <- basicRequest
          .post(uri"/companies")
          .body(CreateCompanyRequest("Rock the JVM", "rockthejvm.com").toJson)
          .header("Authorization", "Bearer ytdyetyue")
          .send(backendStub)
      } yield response.body

      // inspect http response
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[Company].toOption)
          .contains(Company(1, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
      }

    },
    test("get all") {
      val program = for {
        backendStub <- backendSubZIO(_.getAll)
        // run http request
        response <- basicRequest
          .get(uri"/companies")
          .send(backendStub)
      } yield response.body

      // inspect http response
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[List[Company]].toOption)
          .contains(List(rtjvm))
      }

    },
    test("get by id") {
      val program = for {
        backendStub <- backendSubZIO(_.getById)
        // run http request
        response <- basicRequest
          .get(uri"/companies/1")
          .send(backendStub)
      } yield response.body

      // inspect http response
      program.assert { respBody =>
        respBody.toOption
          .flatMap(_.fromJson[Company].toOption)
          .contains(rtjvm)
      }

    }
  ).provide(ZLayer.succeed(serviceStub), stubJwtLayer)
}

package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.controllers.ReviewController
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.services.ReviewService
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

import java.time.Instant

object ReviewControllerSpec extends ZIOSpecDefault {

  private given zioME: MonadError[Task] = new RIOMonadError[Any]

  private val goodReview = Review(
    id = 1L,
    companyId = 1L,
    userId = 1L,
    management = 5,
    culture = 5,
    salary = 5,
    benefits = 5,
    wouldRecommend = 10,
    review = "all good",
    created = Instant.now(),
    updated = Instant.now()
  )

  private val serviceStub = new ReviewService {

    override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
      ZIO.succeed(goodReview)

    override def getById(id: Long): Task[Option[Review]] = {
      ZIO.succeed {
        if (id == 1) Some(goodReview) else None
      }
    }

    override def getByCompanyId(id: Long): Task[List[Review]] = {
      ZIO.succeed {
        if (id == 1) List(goodReview) else List()
      }
    }

    override def getByUserId(id: Long): Task[List[Review]] =
      ZIO.succeed {
        if (id == 1) List(goodReview) else List()
      }
  }

  private def backendSubZIO(endpointFun: ReviewController => ServerEndpoint[Any, Task]) =
    for {
      // create the controller
      controller <- ReviewController.makeZIO
      // build tapir backend
      backendStub <- ZIO.succeed(
        TapirStubInterpreter(SttpBackendStub(zioME))
          .whenServerEndpointRunLogic(endpointFun(controller))
          .backend()
      )
    } yield backendStub

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewControllerSpec")(
      test("create review") {
        val program = for {
          backendStb <- backendSubZIO(_.create)
          response <- basicRequest
            .post(uri"reviews")
            .body(
              CreateReviewRequest(
                companyId = 1L,
                management = 5,
                culture = 5,
                salary = 5,
                benefits = 5,
                wouldRecommend = 10,
                review = "all good"
              ).toJson
            )
            .send(backendStb)
        } yield response.body

        program.assert(
          _.toOption
            .flatMap(_.fromJson[Review].toOption)
            .contains(goodReview)
        )
      },
      test("get by id") {
        for {
          backendStb <- backendSubZIO(_.getById)
          response <- basicRequest
            .get(uri"reviews/1")
            .send(backendStb)
          responseNotFound <- basicRequest
            .get(uri"reviews/100")
            .send(backendStb)
        } yield {
          assertTrue(
            response.body.toOption
              .flatMap(_.fromJson[Review].toOption)
              .contains(goodReview) &&
              responseNotFound.body.toOption
                .flatMap(_.fromJson[Review].toOption)
                .isEmpty
          )
        }
      },
      test("get by company id") {
        for {
          backendStb <- backendSubZIO(_.getByCompanyId)
          response <- basicRequest
            .get(uri"reviews/company/1")
            .send(backendStb)
          responseNotFound <- basicRequest
            .get(uri"reviews/company/100")
            .send(backendStb)
        } yield {
          assertTrue(
            response.body.toOption
              .flatMap(_.fromJson[List[Review]].toOption)
              .contains(List(goodReview)) &&
              responseNotFound.body.toOption
                .flatMap(_.fromJson[List[Review]].toOption)
                .contains(List())
          )
        }
      }
    ).provide(ZLayer.succeed(serviceStub))

}

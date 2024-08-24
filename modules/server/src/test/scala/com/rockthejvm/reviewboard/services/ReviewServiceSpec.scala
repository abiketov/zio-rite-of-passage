package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.repositories.ReviewRepository
import zio.*
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}

import java.time.Instant

object ReviewServiceSpec extends ZIOSpecDefault {

  val goodReview = Review(
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

  val badReview = Review(
    id = 2L,
    companyId = 1L,
    userId = 1L,
    management = 1,
    culture = 1,
    salary = 1,
    benefits = 1,
    wouldRecommend = 1,
    review = "not good",
    created = Instant.now(),
    updated = Instant.now()
  )

  val stubRepoLayer = ZLayer.succeed {
    new ReviewRepository {
      override def create(review: Review): Task[Review] = ZIO.succeed(goodReview)

      override def getById(id: Long): Task[Option[Review]] = {
        ZIO.succeed {
          id match {
            case 1 => Some(goodReview)
            case 2 => Some(badReview)
            case _ => None
          }
        }
      }

      override def getByCompanyId(id: Long): Task[List[Review]] =
        ZIO.succeed {
          id match {
            case 1 => List(goodReview, badReview)
            case _ => List.empty
          }
        }

      override def getByUserId(id: Long): Task[List[Review]] =
        ZIO.succeed {
          id match {
            case 1 => List(goodReview, badReview)
            case _ => List.empty
          }
        }

      override def update(id: Long, op: Review => Review): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"Id not found $id")).map(op)

      override def delete(id: Long): Task[Review] =
        getById(id).someOrFail(new RuntimeException(s"Id not found $id"))
    }
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("ReviewServiceSpec")(
      test("create") {
        for {
          service <- ZIO.service[ReviewService]
          review <- service.create(
            CreateReviewRequest(
              companyId = goodReview.companyId,
              management = goodReview.management,
              culture = goodReview.culture,
              salary = goodReview.salary,
              benefits = goodReview.benefits,
              wouldRecommend = goodReview.wouldRecommend,
              review = goodReview.review
            ),
            userId = 1L
          )

        } yield assertTrue(
          review.companyId == goodReview.companyId &&
            review.management == goodReview.management &&
            review.culture == goodReview.culture &&
            review.salary == goodReview.salary &&
            review.benefits == goodReview.benefits &&
            review.wouldRecommend == goodReview.wouldRecommend &&
            review.review == goodReview.review
        )
      },
      test("get by id") {
        for {
          service        <- ZIO.service[ReviewService]
          review         <- service.getById(1L)
          reviewNotFound <- service.getById(10L)
        } yield assertTrue(
          review.contains(goodReview) &&
            reviewNotFound.isEmpty
        )
      },
      test("get by company id") {
        for {
          service         <- ZIO.service[ReviewService]
          reviews         <- service.getByCompanyId(1L)
          reviewsNotFound <- service.getByCompanyId(10L)
        } yield assertTrue(
          reviews.toSet == Set(goodReview, badReview) &&
            reviewsNotFound.isEmpty
        )
      },
      test("get by user id") {
        for {
          service         <- ZIO.service[ReviewService]
          reviews         <- service.getByUserId(1L)
          reviewsNotFound <- service.getByUserId(10L)
        } yield assertTrue(
          reviews.toSet == Set(goodReview, badReview) &&
            reviewsNotFound.isEmpty
        )
      }
    ).provide(ReviewServiceLive.layer, stubRepoLayer)
  }
}

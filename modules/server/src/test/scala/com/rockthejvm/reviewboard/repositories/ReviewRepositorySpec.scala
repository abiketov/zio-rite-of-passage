package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.repositories.CompanyRepositorySpec.dataSourceLayer
import zio.*
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}
import io.getquill.autoQuote

import java.time.Instant
import com.rockthejvm.reviewboard.syntax.*

object ReviewRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/reviews.sql"

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

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("ReviewRepositorySpec")(
      test("create review") {
        val program = for {
          repo   <- ZIO.service[ReviewRepository]
          review <- repo.create(goodReview)
        } yield review

        program
          .assert { review =>
            review.management == goodReview.management &&
            review.culture == goodReview.culture &&
            review.salary == goodReview.salary &&
            review.benefits == goodReview.benefits &&
            review.wouldRecommend == goodReview.wouldRecommend &&
            review.review == goodReview.review
          }
      },
      test("get review by ids (id, companyId, userId)") {
        for {
          repo           <- ZIO.service[ReviewRepository]
          review         <- repo.create(goodReview)
          fetchedReview1 <- repo.getById(review.id)
          fetchedReview2 <- repo.getByCompanyId(review.companyId)
          fetchedReview3 <- repo.getByUserId(review.userId)
        } yield assertTrue(
          fetchedReview1 == Some(review) &&
            fetchedReview2.contains(review) &&
            fetchedReview3.contains(review)
        )
      },
      test("get all") {
        for {
          repo           <- ZIO.service[ReviewRepository]
          review1        <- repo.create(goodReview)
          review2        <- repo.create(badReview)
          reviewsCompany <- repo.getByCompanyId(review1.companyId)
          reviewsUser    <- repo.getByUserId(review1.userId)
        } yield assertTrue(
          reviewsCompany.toSet == Set(review1, review2) &&
            reviewsUser.toSet == Set(review1, review2)
        )
      },
      test("edit review") {
        for {
          repo    <- ZIO.service[ReviewRepository]
          review  <- repo.create(goodReview)
          updated <- repo.update(review.id, review => review.copy(review = "very good"))
        } yield assertTrue(
          review.id == updated.id &&
            review.companyId == updated.companyId &&
            review.userId == updated.userId &&
            review.management == updated.management &&
            review.culture == updated.culture &&
            review.salary == updated.salary &&
            review.benefits == updated.benefits &&
            review.wouldRecommend == updated.wouldRecommend &&
            updated.review == "very good" &&
            review.created == updated.created &&
            review.updated != updated.updated
        )
      },
      test("delete review") {
        for {
          repo        <- ZIO.service[ReviewRepository]
          review      <- repo.create(goodReview)
          deleted     <- repo.delete(review.id)
          maybeReview <- repo.getById(review.id)
        } yield assertTrue(maybeReview.isEmpty)
      }
    ).provide(
      ReviewRepositoryLive.layer,
      dataSourceLayer,
      Repository.quillLayer,
      Scope.default
    )

}

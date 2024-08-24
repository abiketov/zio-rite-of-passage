package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.Review
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import com.rockthejvm.reviewboard.repositories.{CompanyRepository, ReviewRepository}
import zio.*

import java.time.Instant

trait ReviewService {

  def create(request: CreateReviewRequest, userId: Long): Task[Review]
  def getById(id: Long): Task[Option[Review]]
  def getByCompanyId(id: Long): Task[List[Review]]
  def getByUserId(id: Long): Task[List[Review]]
  // def update(id: Long, op: Review => Review): Task[Review]
  // def delete(id: Long): Task[Review]
}

class ReviewServiceLive private (repo: ReviewRepository) extends ReviewService {

  override def create(request: CreateReviewRequest, userId: Long): Task[Review] =
    repo.create(
      Review(
        id = -1L,
        companyId = request.companyId,
        userId = userId,
        management = request.management,
        culture = request.culture,
        salary = request.salary,
        benefits = request.benefits,
        wouldRecommend = request.wouldRecommend,
        review = request.review,
        created = Instant.now(),
        updated = Instant.now()
      )
    )
  override def getById(id: Long): Task[Option[Review]] =
    repo.getById(id)
  override def getByCompanyId(id: Long): Task[List[Review]] =
    repo.getByCompanyId(id)
  override def getByUserId(id: Long): Task[List[Review]] =
    repo.getByUserId(id)
}

object ReviewServiceLive {

  val layer = ZLayer {
    for {
      repo <- ZIO.service[ReviewRepository]
    } yield new ReviewServiceLive(repo)
  }
}

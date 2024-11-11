package com.rockthejvm.reviewboard.http.requests

import zio.json.{DeriveJsonCodec, JsonCodec}
import com.rockthejvm.reviewboard.domain.data.*

final case class CreateReviewRequest(
    companyId: Long,
    management: Int,
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String
)

object CreateReviewRequest {
  given codec: JsonCodec[CreateReviewRequest] = DeriveJsonCodec.gen[CreateReviewRequest]

  def fromReview(userReview: Review): CreateReviewRequest = {

    CreateReviewRequest(
      companyId = userReview.companyId,
      management = userReview.management,
      culture = userReview.culture,
      salary = userReview.salary,
      benefits = userReview.benefits,
      wouldRecommend = userReview.wouldRecommend,
      review = userReview.review
    )
  }
}

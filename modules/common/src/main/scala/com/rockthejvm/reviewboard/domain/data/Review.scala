package com.rockthejvm.reviewboard.domain.data

import java.time.Instant
import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Review(
    id: Long,
    companyId: Long,
    userId: Long,
    management: Int,
    culture: Int,
    salary: Int,
    benefits: Int,
    wouldRecommend: Int,
    review: String,
    created: Instant,
    updated: Instant
)

object Review {

  given codec: JsonCodec[Review] = DeriveJsonCodec.gen[Review]

  def empty(companyId: Long) =
    Review(-1L, companyId, -1L, 5, 5, 5, 5, 5, "", Instant.now, Instant.now)
}

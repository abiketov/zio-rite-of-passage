package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest

trait ReviewEndpoints extends BaseEndPoint {

  // post /reviews - create review { CreateReviewRequest }
  // returns a Review
  val createEndpoint = baseEndPoint
    .tag("Reviews")
    .name("create")
    .description("Add a review for a company")
    .in("reviews")
    .post
    .in(jsonBody[CreateReviewRequest])
    .out(jsonBody[Review])
  // get /reviews/id - get review by id
  // returns Option[Review]
  val getByIdEndpoint = baseEndPoint
    .tag("Reviews")
    .name("getById")
    .description("Get a review by its id")
    .in("reviews" / path[Long]("id"))
    .get
    .out(jsonBody[Option[Review]])

  // get /reviews/company/id - get reviews by company id
  // returns List[Review]
  val getCompanyIdEndpoint = baseEndPoint
    .tag("Reviews")
    .name("getByCompanyId")
    .description("Get a review by company id")
    .in("reviews" / "company" / path[Long]("id"))
    .get
    .out(jsonBody[List[Review]])

}

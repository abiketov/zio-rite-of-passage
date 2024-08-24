package com.rockthejvm.reviewboard.http.controllers

import zio.{Task, ZIO}
import sttp.tapir.server.ServerEndpoint
import com.rockthejvm.reviewboard.http.endpoints.ReviewEndpoints
import com.rockthejvm.reviewboard.services.ReviewService

class ReviewController private (reviewService: ReviewService)
    extends BaseController
    with ReviewEndpoints {

  val create: ServerEndpoint[Any, Task] =
    createEndpoint.serverLogic(req => reviewService.create(req, -1L).either)

  val getById: ServerEndpoint[Any, Task] =
    getByIdEndpoint.serverLogic(id => reviewService.getById(id).either)

  val getByCompanyId: ServerEndpoint[Any, Task] =
    getCompanyIdEndpoint.serverLogic(companyId => reviewService.getByCompanyId(companyId).either)

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(create, getById, getByCompanyId)
}

object ReviewController {
  val makeZIO = for {
    service <- ZIO.service[ReviewService]
  } yield new ReviewController(service)
}

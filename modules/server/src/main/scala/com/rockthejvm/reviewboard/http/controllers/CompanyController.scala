package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.*
import com.rockthejvm.reviewboard.services.CompanyService
import scala.collection.mutable
import io.getquill.autoQuote

class CompanyController private (service: CompanyService)
    extends BaseController
    with CompanyEndpoints {

  val create: ServerEndpoint[Any, Task] = createEndpoint.serverLogic { req =>
    service.create(req).either
  }

  val getAll: ServerEndpoint[Any, Task] =
    getAllEndpoint.serverLogic(_ => service.getAll.either)

  val getById: ServerEndpoint[Any, Task] = getByIdEndpoint.serverLogic { id =>
    ZIO
      .attempt(id.toLong)
      .flatMap(service.getById)
      .catchSome { case _: java.lang.NumberFormatException =>
        service.getBySlug(id)
      }
      .either
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(create, getAll, getById)
}

object CompanyController {
  val makeZIO = for {
    service <- ZIO.service[CompanyService]
  } yield new CompanyController(service)
}

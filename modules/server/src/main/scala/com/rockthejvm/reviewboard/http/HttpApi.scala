package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.{
  BaseController,
  CompanyController,
  HealthController,
  ReviewController
}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

object HttpApi {

  def makeControllers = for {
    health    <- controllers.HealthController.makeZIO
    companies <- CompanyController.makeZIO
    reviews   <- ReviewController.makeZIO
  } yield {
    List(health, companies, reviews)
  }

  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}

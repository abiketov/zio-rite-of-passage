package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.{
  BaseController,
  CompanyController,
  HealthController
}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

object HttpApi {

  def makeControllers = for {
    health    <- controllers.HealthController.makeZIO
    companies <- CompanyController.makeZIO
  } yield {
    println("here")
    List(health, companies)
  }

  def gatherRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  val endpointsZIO = makeControllers.map(gatherRoutes)
}

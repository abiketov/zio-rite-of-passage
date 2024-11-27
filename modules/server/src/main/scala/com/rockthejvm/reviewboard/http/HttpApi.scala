package com.rockthejvm.reviewboard.http

import com.rockthejvm.reviewboard.http.controllers.{
  BaseController,
  CompanyController,
  HealthController,
  InviteController,
  ReviewController,
  UserController
}
import com.rockthejvm.reviewboard.http.endpoints.BaseEndPoint
import com.rockthejvm.reviewboard.services.{CompanyService, JWTService, ReviewService, UserService}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ZIO}

object HttpApi {

  def makeControllers = for {
    health    <- HealthController.makeZIO
    companies <- CompanyController.makeZIO
    reviews   <- ReviewController.makeZIO
    users     <- UserController.makeZIO
    invites   <- InviteController.makeZIO
  } yield {
    List(health, companies, reviews, users, invites)
  }

  def collectRoutes(controllers: List[BaseController]) =
    controllers.flatMap(_.routes)

  val endpointsZIO = makeControllers.map(collectRoutes)
}

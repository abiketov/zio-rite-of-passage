package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.http.HttpApi
import zio.*
import sttp.tapir.*
import zio.http.Server
import sttp.tapir.server.ziohttp.*
import com.rockthejvm.reviewboard.http.controllers.*
import com.rockthejvm.reviewboard.services.CompanyService

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    server <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default
      ).toHttp(endpoints)
    )
  } yield ()
  override def run = serverProgram.provide(
    Server.default,
    CompanyService.dummyLayer
  )

}

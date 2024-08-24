package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.services.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import sttp.tapir.*
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

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
    CompanyServiceLive.layer,
    ReviewServiceLive.layer,
    ReviewRepositoryLive.layer,
    CompanyRepositoryLive.layer,
    Repository.dataLayer,
  )

}

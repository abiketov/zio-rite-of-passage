package com.rockthejvm.reviewboard

import com.rockthejvm.reviewboard.config.{Configs, JWTConfig}
import com.rockthejvm.reviewboard.http.HttpApi
import com.rockthejvm.reviewboard.repositories.*
import com.rockthejvm.reviewboard.services.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import sttp.tapir.*
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.*
import zio.*
import zio.http.Server

object Application extends ZIOAppDefault {

  val serverProgram = for {
    endpoints <- HttpApi.endpointsZIO
    server <- Server.serve(
      ZioHttpInterpreter(
        ZioHttpServerOptions.default.appendInterceptor(CORSInterceptor.default)
      ).toHttp(endpoints)
    )
  } yield ()
  override def run = serverProgram.provide(
    Server.default,
    // services
    CompanyServiceLive.layer,
    ReviewServiceLive.layer,
    UserServiceLive.layer,
    JWTServiceLive.configuredLayer,
    EmailServiceLive.configuredLayer,
    // repositories
    ReviewRepositoryLive.layer,
    CompanyRepositoryLive.layer,
    UserRepositoryLive.layer,
    RecoveryTokensRepositoryLive.configuredLayer,
    Repository.dataLayer
  )

}

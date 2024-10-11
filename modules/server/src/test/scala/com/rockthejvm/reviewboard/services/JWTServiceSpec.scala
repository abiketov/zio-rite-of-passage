package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.JWTConfig
import com.rockthejvm.reviewboard.domain.data.User
import zio.*
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}

object JWTServiceSpec extends ZIOSpecDefault {

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("JWTServiceSpec")(
      test("create and validate token") {
        for {
          service   <- ZIO.service[JWTService]
          userToken <- service.createToken(User(1L, "daniel@rockthejvm.com", "unimportant"))
          userId    <- service.verifyToken(userToken.token)
        } yield assertTrue(
          userId.id == 1L &&
            userId.email == "daniel@rockthejvm.com"
        )
      }
    ).provide(
      ZLayer.succeed(JWTConfig("secret", 3600)),
      JWTServiceLive.layer
    )
  }
}

package com.rockthejvm.reviewboard.services

import zio.*
import com.auth0.jwt.*
import com.auth0.jwt.JWTVerifier.BaseVerification
import com.auth0.jwt.algorithms.Algorithm
import com.rockthejvm.reviewboard.config.{Configs, JWTConfig}
import com.rockthejvm.reviewboard.domain.data.{User, UserId, UserToken}
import com.typesafe.config.ConfigFactory
import zio.config.typesafe.TypesafeConfig

import java.time.Instant

trait JWTService {

  def createToken(user: User): Task[UserToken]
  def verifyToken(token: String): Task[UserId]
}

class JWTServiceLive(jwtConfig: JWTConfig, clock: java.time.Clock) extends JWTService {

  private val secret     = jwtConfig.secret // Pass from config
  private val issuer     = "rockthejvm"
  private val timeToLive = jwtConfig.timeToLive
  private val username   = "username"
  private val algorithm  = Algorithm.HMAC512(secret)

  private val verifier: JWTVerifier = JWT
    .require(algorithm)
    .withIssuer(issuer)
    .asInstanceOf[BaseVerification]
    .build(clock)

  override def createToken(user: User): Task[UserToken] =
    for {
      now        <- ZIO.attempt(clock.instant)
      expiration <- ZIO.succeed(now.plusSeconds(timeToLive))
      token <- ZIO.attempt {
        JWT.create
          .withIssuer(issuer)
          .withIssuedAt(now)
          .withExpiresAt(expiration)
          .withSubject(user.id.toString)
          .withClaim(username, user.email)
          .sign(algorithm)
      }
    } yield UserToken(user.email, token, expiration.getEpochSecond)

  override def verifyToken(token: String): Task[UserId] =
    for {
      decoded <- ZIO.attempt(verifier.verify(token))
      userId <- ZIO.attempt(
        UserId(decoded.getSubject().toLong, decoded.getClaim(username).asString())
      )
    } yield userId
}

object JWTServiceLive {

  val layer = ZLayer {
    for {
      jwtConfig <- ZIO.service[JWTConfig]
      clock     <- Clock.javaClock
    } yield new JWTServiceLive(jwtConfig, clock)
  }

  val configuredLayer = Configs.makeLayer[JWTConfig]("rockthejvm.jwt") >>> layer
}

object JWTServiceDemo extends ZIOAppDefault {

  val program = for {
    service   <- ZIO.service[JWTService]
    userToken <- service.createToken(User(1L, "daniel@rockthejvm.com", "notimportanthere"))
    _         <- Console.printLine(userToken)
    userId    <- service.verifyToken(userToken.token)
    _         <- Console.printLine(userId.toString)
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(
      JWTServiceLive.layer,
      Configs.makeLayer[JWTConfig]("rockthejvm.jwt")
    )

//  def main(args: Array[String]): Unit = {
//
//    val secret     = "secret" // Pass from config
//    val issuer     = "rockthejvm"
//    val timeToLive = 30 * 24 * 3600
//    val username   = "username"
//    val algorithm  = Algorithm.HMAC512(secret)
//
//    val jwt = JWT.create
//      .withIssuer(issuer)
//      .withIssuedAt(Instant.now())
//      .withExpiresAt(Instant.now().plusSeconds(timeToLive))
//      .withSubject("1")
//      .withClaim(username, "daniel@rockthejvm.com")
//      .sign(algorithm)
//
//    val verifier: JWTVerifier = JWT
//      .require(algorithm)
//      .withIssuer(issuer)
//      .asInstanceOf[BaseVerification]
//      .build(java.time.Clock.systemDefaultZone())
//
//    val decoded   = verifier.verify(jwt)
//    val userId    = decoded.getSubject
//    val userEmail = decoded.getClaim(username)
//    println(userId)
//    println(userEmail)
//  }
}

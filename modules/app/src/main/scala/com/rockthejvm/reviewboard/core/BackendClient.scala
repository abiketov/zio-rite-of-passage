package com.rockthejvm.reviewboard.core

import com.rockthejvm.reviewboard.http.endpoints.{CompanyEndpoints, ReviewEndpoints, UserEndpoints}
import com.rockthejvm.reviewboard.config.BackendClientConfig
import sttp.capabilities
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.impl.zio.FetchZioBackend
import sttp.client3.{Request, SttpBackend, UriContext}
import sttp.model.Uri
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

case class RestrictedEndpointException(msg: String) extends RuntimeException(msg)

trait BackendClient {

  val companyEndpoints: CompanyEndpoints

  val userEndpoints: UserEndpoints

  val reviewEndpoints: ReviewEndpoints

  def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(payload: I): Task[O]

  def secureEndpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient {

  override val companyEndpoints: CompanyEndpoints = new CompanyEndpoints {}
  override val userEndpoints: UserEndpoints       = new UserEndpoints {}
  override val reviewEndpoints: ReviewEndpoints   = new ReviewEndpoints {}

  private def endpointRequest[I, E, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] = {
    interpreter.toRequestThrowDecodeFailures(endpoint, config.uri)
  }

  private def secureEndpointRequest[S, I, E, O](
      endpoint: Endpoint[S, I, E, O, Any]
  ): S => I => Request[Either[E, O], Any] = {
    interpreter.toSecureRequestThrowDecodeFailures(endpoint, config.uri)
  }

  override def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(payload: I): Task[O] = {
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve
  }

  private def tokenOrFail: ZIO[Any, RestrictedEndpointException, String] = ZIO
    .fromOption(Session.getUserState)
    .orElseFail(RestrictedEndpointException("You need to login"))
    .map(_.token)

  override def secureEndpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[String, I, E, O, Any]
  )(payload: I): Task[O] =
    for {
      token    <- tokenOrFail
      response <- backend.send(secureEndpointRequest(endpoint)(token)(payload)).map(_.body).absolve
    } yield response

}

object BackendClientLive {
  val layer = ZLayer {
    for {
      backend     <- ZIO.service[SttpBackend[Task, ZioStreams & capabilities.WebSockets]]
      interpreter <- ZIO.service[SttpClientInterpreter]
      config      <- ZIO.service[BackendClientConfig]
    } yield new BackendClientLive(backend, interpreter, config)
  }

  val configuredLayer = {
    val backend: SttpBackend[Task, ZioStreams & capabilities.WebSockets] = FetchZioBackend()
    val interpreter: SttpClientInterpreter                               = SttpClientInterpreter()
    val config = BackendClientConfig(Some(uri"http://localhost:8080"))
    ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter) ++ ZLayer.succeed(config) >>> layer
  }
}

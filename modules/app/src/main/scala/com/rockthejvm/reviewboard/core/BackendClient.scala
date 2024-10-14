package com.rockthejvm.reviewboard.core

import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
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

trait BackendClient {
  val companyEndpoints: CompanyEndpoints

  def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig
) extends BackendClient {

  override val companyEndpoints: CompanyEndpoints = new CompanyEndpoints {}

  private def endpointRequest[I, E, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  ): I => Request[Either[E, O], Any] = {
    interpreter.toRequestThrowDecodeFailures(endpoint, config.uri)
  }

  override def endpointRequestZIO[I, E <: Throwable, O](
      endpoint: Endpoint[Unit, I, E, O, Any]
  )(payload: I): Task[O] = {
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve
  }

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

package com.rockthejvm.reviewboard.http.controllers

import com.rockthejvm.reviewboard.domain.data.{InviteNamedRecord, UserId}
import com.rockthejvm.reviewboard.http.endpoints.InviteEndpoints
import com.rockthejvm.reviewboard.http.requests.InvitePackRequest
import com.rockthejvm.reviewboard.http.responses.InviteResponse
import sttp.tapir.server.ServerEndpoint
import zio.*
import com.rockthejvm.reviewboard.services.*
import sttp.tapir.server.ServerEndpoint.Full

class InviteController private (
    inviteService: InviteService,
    jwtService: JWTService,
    paymentService: PaymentService
) extends BaseController
    with InviteEndpoints {

  val addPack: ServerEndpoint[Any, zio.Task] = addPackEndpoint
    .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { token => request =>
      inviteService
        .addInvitePack(token.email, request.companyId)
        .map(_.toString)
        .either
    }

  val invite: ServerEndpoint[Any, zio.Task] = inviteEndpoint
    .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
    .serverLogic { token => req =>
      inviteService
        .sendInvites(token.email, req.companyId, req.emails)
        .map { nInvitesSent =>
          if (nInvitesSent == req.emails.size)
            InviteResponse(status = "OK", nInvitesSent)
          else InviteResponse("Partial success", nInvites = nInvitesSent)
        }
        .either
    }

  val getInvitesByUserId: ServerEndpoint[Any, zio.Task] =
    getInvitesByUserIdEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { token => _ =>
        inviteService.getInvitesByUserName(token.email).either
      }

  val addPackPromoted: ServerEndpoint[Any, zio.Task] =
    addPackPromotedEndpoint
      .serverSecurityLogic[UserId, Task](token => jwtService.verifyToken(token).either)
      .serverLogic { token => req =>
        inviteService
          .addInvitePack(token.email, req.companyId)
          .flatMap { packId =>
            paymentService.createCheckoutSession(packId, token.email)
          }
          .someOrFail(new RuntimeException("Cannot create payment checkout session"))
          .map(_.getUrl())
          .either
      }

  val webhook: ServerEndpoint[Any, zio.Task] = webhookEndpoint.serverLogic { (signature, payload) =>
    paymentService
      .handleWebhookEvent(
        signature,
        payload,
        packId => inviteService.activatePack(packId.toLong)
      )
      .unit
      .either
  }

  override val routes: List[ServerEndpoint[Any, Task]] =
    List(addPack, invite, webhook, getInvitesByUserId, addPackPromoted)
}

object InviteController {

  val makeZIO = for {
    inviteService  <- ZIO.service[InviteService]
    jwtService     <- ZIO.service[JWTService]
    paymentService <- ZIO.service[PaymentService]
  } yield new InviteController(inviteService, jwtService, paymentService)
}

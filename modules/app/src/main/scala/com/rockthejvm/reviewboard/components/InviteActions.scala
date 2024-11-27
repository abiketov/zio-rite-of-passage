package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.common.Constants
import org.scalajs.dom.html.Element
import org.scalajs.dom
import zio.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.domain.*
import com.rockthejvm.reviewboard.domain.data.*

object InviteActions {

  private val inviteListBus = EventBus[List[InviteNamedRecord]]()

  private def refreshInviteList() = {

    useBackend(_.inviteEndpoints.getInvitesByUserIdEndpoint(()))
  }

  private def renderInviteRecord(record: InviteNamedRecord) = {

    val emailListVar  = Var[Array[String]](Array())
    val maybeErrorVar = Var[Option[String]](None)
    val inviteSubmitter = Observer[Unit] { _ =>
      val emailList = emailListVar.now().toList
      if (emailList.exists(!_.matches(Constants.emailRegex))) {
        maybeErrorVar.set(Some("At least one an email is invalid"))
      } else {
        // invite emails
        // refresh invite list
        val refreshProgram = for {
          _ <- useBackend(
            _.inviteEndpoints.inviteEndpoint(InviteRequest(record.companyId, emailList))
          )
          invitesLeft <- refreshInviteList()
        } yield invitesLeft

        refreshProgram.emitTo(inviteListBus)
        maybeErrorVar.set(None)
      }
    }

    div(
      cls := "invite-record",
      h5(span(record.companyName)),
      p(s"Invites left: ${record.nInvites}"),
      textArea(
        cls         := "invites-area",
        placeholder := "Entre emails, one per line",
        onInput.mapToValue.map(_.split("\n").map(_.trim).filter(_.nonEmpty)) --> emailListVar.writer
      ),
      button(
        `type` := "button",
        cls    := "btn btn-primary",
        "Invite",
        // TODO: trigger backend call
        onClick.mapToUnit --> inviteSubmitter
      ),
      child.maybe <-- maybeErrorVar.signal.map(maybeRenderError)
    )
    // Company name
    // number of invites left
    // text area - email addresses one per line
    // button to send invites
    // show error status

  }

  private def maybeRenderError(maybeError: Option[String]) = {
    maybeError.map { message =>
      div(
        cls := "invite-error",
        message
      )
    }
  }

  def apply() = {

    div(
      onMountCallback(_ => refreshInviteList().emitTo(inviteListBus)),
      cls := "profile-section",
      h3(span("Invite Actions")),
      children <-- inviteListBus.events.map(_.sortBy(_.companyName)).map(renderInviteRecord))
    )
  }
}

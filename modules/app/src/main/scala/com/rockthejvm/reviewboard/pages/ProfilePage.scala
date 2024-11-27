package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.common.Constants
import org.scalajs.dom.html.Element
import org.scalajs.dom
import zio.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.http.requests.UpdatePasswordRequest
import com.rockthejvm.reviewboard.components.*

object ProfilePage {

  def apply() = {

    div(
      cls := "row",
      div(
        cls := "col-md-5 p-0",
        div(
          cls := "logo",
          img(
            src := Constants.logoImage,
            alt := "Rock the JVM"
          )
        )
      ),
      div(
        cls := "col-md-7",
        // right
        div(
          cls := "form-section",
          child <-- Session.userState.signal.map {
            case None    => renderInvalid()
            case Some(_) => renderContent()
          }
        )
      )
    )
  }

  private def renderInvalid() = {

    div(
      cls := "top-section",
      h1(span("Ooops!")),
      div("You need to login")
    )
  }

  private def renderContent() = {

    div(
      cls := "top-section",
      h1(span("Profile")),
      // change password section
      div(
        cls := "profile-section",
        h3(span("Account Settings")),
        Anchors.renderNavLink("Change Password", "/changepassword")
      ),
      // actions section - to send invites to every company they have invites for
      InviteActions()
    )
  }
}

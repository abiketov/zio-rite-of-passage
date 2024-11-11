package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Element
import org.scalajs.dom
import zio.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.components.Anchors
import com.rockthejvm.reviewboard.http.requests.RecoverPasswordRequest

case class RecoverPasswordPageState(
    email: String = "",
    token: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {

  override def mayBeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

  override def errorList: List[Option[String]] = List(
    Option.when(!email.matches(Constants.emailRegex))("Email is invalid"),
    Option.when(token.isEmpty)("Token can't be empty"),
    Option.when(newPassword.isEmpty)("Password can't be empty"),
    Option.when(newPassword != confirmPassword)("Passwords must match")
  ) ++ upstreamStatus.map(_.left.toOption).toList
}

object RecoverPasswordPage extends FormPage[RecoverPasswordPageState]("Reset Password") {

  override def basicState = RecoverPasswordPageState()

  val submitter = Observer[RecoverPasswordPageState] { state =>
    // check state error
    if (state.hasErrors) {
      // if error - show
      stateVar.update(_.copy(showStatus = true))
    } else {
      dom.console.log(s"Current state is: $state, trigger backend call")
      // no errors - trigger backend
      useBackend(
        _.userEndpoints.recoverPasswordEndpoint(
          RecoverPasswordRequest(state.email, state.token, state.newPassword)
        )
      )
        .map { _ =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Right("Password reset successfully!"))
            )
          )
        }
        .tapError { e =>
          ZIO.succeed(
            stateVar.update(_.copy(showStatus = true, upstreamStatus = Some(Left(e.getMessage))))
          )
        }
        .runJs()
    }

    // if backend returns error - shoe error
    // if success - set user token, navigate to home page
    dom.console.log(s"Current state is: $state")

  }

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Recovery Token",
      "token-input",
      "text",
      true,
      "Password recovery token",
      (s, t) => s.copy(token = t, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "New Password",
      "new-password-input",
      "password",
      true,
      "Your new password",
      (s, p) => s.copy(newPassword = p, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Confirm Password",
      "confirm-password-input",
      "password",
      true,
      "Confirm password",
      (s, p) => s.copy(confirmPassword = p, showStatus = false, upstreamStatus = None)
    ),
    button(
      `type` := "button",
      "Reset Password",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    ),
    div(
      cls := "auth-link",
      Anchors.renderNavLink("Need a password recovery token?", "/forgot")
    )
  )
}

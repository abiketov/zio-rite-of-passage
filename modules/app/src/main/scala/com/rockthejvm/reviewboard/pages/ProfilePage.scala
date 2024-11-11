package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Element
import org.scalajs.dom
import zio.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.http.requests.UpdatePasswordRequest

case class ProfilePageState(
    password: String = "",
    newPassword: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {

  override def mayBeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

  override def errorList: List[Option[String]] = List(
    Option.when(password.isEmpty || newPassword.isEmpty)("Password can't be empty"),
    Option.when(newPassword != confirmPassword)("Passwords must match")
  ) ++ upstreamStatus.map(_.left.toOption).toList
}
object ProfilePage extends FormPage[ProfilePageState]("User Profile") {

  override def basicState = ProfilePageState()

  def submitter(email: String) = Observer[ProfilePageState] { state =>
    // check state error
    if (state.hasErrors) {
      // if error - show
      stateVar.update(_.copy(showStatus = true))
    } else {
      dom.console.log(s"Current state is: $state, trigger backend call")
      // no errors - trigger backend
      useBackend(
        _.userEndpoints.updatePasswordEndpoint(
          UpdatePasswordRequest(email, state.password, state.newPassword)
        )
      )
        .map { userResponse =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Right("Your password was updated."))
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

  override def renderChildren(): List[ReactiveHtmlElement[Element]] =
    Session.getUserState
      .map(_.email)
      .map(email =>
        List(
          renderInput(
            "Current Password",
            "password-input",
            "password",
            true,
            "Your current password",
            (s, p) => s.copy(password = p, showStatus = false, upstreamStatus = None)
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
            "Change Password",
            onClick.preventDefault.mapTo(stateVar.now()) --> submitter(email)
          )
        )
      )
      .getOrElse(
        List(
          div(
            cls := "logout-status",
            "You must be logged in to change your profile."
          )
        )
      )
}

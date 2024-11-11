package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.http.requests.RegisterUserAccount
import org.scalajs.dom
import org.scalajs.dom.html
import zio.*

case class SignUpFormState(
    email: String = "",
    password: String = "",
    confirmPassword: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {

  private val emailFormatError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("Email is invalid")

  private val passwordError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")

  private val confirmPasswordError: Option[String] =
    Option.when(password != confirmPassword)("Passwords must match")

  override def mayBeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

  override def errorList: List[Option[String]] =
    List(emailFormatError, passwordError, confirmPasswordError) ++ upstreamStatus
      .map(_.left.toOption)
      .toList
}

object SignupPage extends FormPage[SignUpFormState]("Sign Up") {

  override def basicState = SignUpFormState()

  val submitter = Observer[SignUpFormState] { state =>
    // check state error
    if (state.hasErrors) {
      // if error - show
      stateVar.update(_.copy(showStatus = true))
    } else {
      dom.console.log(s"Current state is: $state, trigger backend call")
      // no errors - trigger backend
      useBackend(
        _.userEndpoints.createUserEndpoint(RegisterUserAccount(state.email, state.password))
      )
        .map { user =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Right("Account created. You can login now."))
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

  override def renderChildren(): List[ReactiveHtmlElement[html.Element]] = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upstreamStatus = None)
    ),
    // an input of type password
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, p) => s.copy(password = p, showStatus = false, upstreamStatus = None)
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
      "Sign Up",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )
}

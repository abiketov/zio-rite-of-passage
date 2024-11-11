package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{div, *, given}
import com.rockthejvm.reviewboard.common.Constants
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.components.*
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import com.rockthejvm.reviewboard.http.requests.LoginRequest
import org.scalajs.dom
import frontroute.*

case class LoginFormState(
    email: String = "",
    password: String = "",
    upstreamError: Option[String] = None,
    override val showStatus: Boolean = false
) extends FormState {

  private val emailFormatError: Option[String] =
    Option.when(!email.matches(Constants.emailRegex))("Email is invalid")

  private val passwordError: Option[String] =
    Option.when(password.isEmpty)("Password can't be empty")

  override val errorList = List(emailFormatError, passwordError, upstreamError)

  override def mayBeSuccess: Option[String] = None

}

object LoginPage extends FormPage[LoginFormState]("Log In") {

  override def basicState = LoginFormState()

  val submitter = Observer[LoginFormState] { state =>
    // check state error
    if (state.hasErrors) {
      // if error - show
      stateVar.update(_.copy(showStatus = true))
    } else {
      dom.console.log(s"Current state is: $state, trigger backend call")
      // no errors - trigger backend
      useBackend(_.userEndpoints.loginEndpoint(LoginRequest(state.email, state.password)))
        .map { userToken =>
          // if success, set user token,navigate away
          // TODO set user token
          Session.setUserState(userToken)
          stateVar.set(LoginFormState())
          BrowserNavigation.replaceState("/")
        }
        .tapError { e =>
          ZIO.succeed(
            stateVar.update(_.copy(showStatus = true, upstreamError = Some(e.getMessage)))
          )
        }
        .runJs()
    }

    // if backend returns error - shoe error
    // if success - set user token, navigate to home page
    dom.console.log(s"Current state is: $state")

  }

  def renderChildren() = List(
    renderInput(
      "Email",
      "email-input",
      "text",
      true,
      "Your email",
      (s, e) => s.copy(email = e, showStatus = false, upstreamError = None)
    ),
    // an input of type password
    renderInput(
      "Password",
      "password-input",
      "password",
      true,
      "Your password",
      (s, p) => s.copy(password = p, showStatus = false, upstreamError = None)
    ),
    button(
      `type` := "button",
      "Log In",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    ),
    div(
      cls := "auth-link",
      Anchors.renderNavLink("Forgot password?", "/forgot")
    )
  )

//  def maybeRenderSuccess(shouldShow: Boolean = false) = {
//
//    if (shouldShow) {
//      div(
//        cls := "page-status-success",
//        child.text <-- stateVar.signal.map(_.toString)
//      )
//    } else div()
//
//  }
//
//  def renderInput(
//      name: String,
//      uid: String,
//      kind: String,
//      isRequired: Boolean,
//      placeHolder: String,
//      updateFn: (LoginFormState, String) => LoginFormState
//  ) = {
//    div(
//      cls := "row",
//      div(
//        cls := "col-md-12",
//        div(
//          cls := "form-input",
//          label(
//            forId := uid,
//            cls   := "form-label",
//            if (isRequired) span("*") else span(),
//            name
//          ),
//          input(
//            `type`      := kind,
//            cls         := "form-control",
//            idAttr      := uid,
//            placeholder := placeHolder,
//            onInput.mapToValue --> stateVar.updater(updateFn)
//          )
//        )
//      )
//    )
//  }
//
//  def renderError(error: String) = {
//    div(
//      cls := "page-status-errors",
//      error
//    )
//  }
}

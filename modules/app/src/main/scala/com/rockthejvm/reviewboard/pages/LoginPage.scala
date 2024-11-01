package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{div, *, given}
import com.rockthejvm.reviewboard.common.Constants
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import com.rockthejvm.reviewboard.http.requests.LoginRequest
import org.scalajs.dom
import frontroute.*

object LoginPage {

  case class State(
      email: String = "",
      password: String = "",
      upstreamError: Option[String] = None,
      showStatus: Boolean = false
  ) {
    val emailFormatError: Option[String] =
      Option.when(!email.matches(Constants.emailRegex))("Email is invalid")
    val passwordError: Option[String] = Option.when(password.isEmpty)("Password can't be empty")

    val errorList  = List(emailFormatError, passwordError, upstreamError)
    val mayBeError = errorList.find(_.isDefined).flatten.filter(_ => showStatus)
    val hasErrors  = errorList.exists(_.isDefined)
  }

  val stateVar = Var[State](State())
  val submitter = Observer[State] { state =>
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
          stateVar.set(State())
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
          div(cls := "top-section", h1(span("Log In"))),
          children <-- stateVar.signal
            .map(_.mayBeError)
            .map(_.map(renderError))
            .map(_.toList),
          maybeRenderSuccess(),
          form(
            nameAttr := "signin",
            cls      := "form",
            idAttr   := "form",
            // an input of type text
            renderChildren()
          )
        )
      )
    )
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
    )
  )

  def maybeRenderSuccess(shouldShow: Boolean = false) = {

    if (shouldShow) {
      div(
        cls := "page-status-success",
        child.text <-- stateVar.signal.map(_.toString)
      )
    } else div()

  }

  def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      placeHolder: String,
      updateFn: (State, String) => State
  ) = {
    div(
      cls := "row",
      div(
        cls := "col-md-12",
        div(
          cls := "form-input",
          label(
            forId := uid,
            cls   := "form-label",
            if (isRequired) span("*") else span(),
            name
          ),
          input(
            `type`      := kind,
            cls         := "form-control",
            idAttr      := uid,
            placeholder := placeHolder,
            onInput.mapToValue --> stateVar.updater(updateFn)
          )
        )
      )
    )
  }

  def renderError(error: String) = {
    div(
      cls := "page-status-errors",
      error
    )
  }
}

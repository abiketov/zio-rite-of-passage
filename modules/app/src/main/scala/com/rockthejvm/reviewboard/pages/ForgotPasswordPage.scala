package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.components.Anchors
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.http.requests.*
import org.scalajs.dom.html.Element
import org.scalajs.dom
import zio.*

case class ForgotPasswordPageState(
    email: String = "",
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {

  override def mayBeSuccess: Option[String] = upstreamStatus.flatMap(_.toOption)

  override val errorList: List[Option[String]] = List(
    Option.when(!email.matches(Constants.emailRegex))("Email is invalid")
  ) ++ upstreamStatus.map(_.left.toOption).toList
}

object ForgotPasswordPage extends FormPage[ForgotPasswordPageState]("Password recovery") {

  override def basicState: ForgotPasswordPageState = ForgotPasswordPageState()

  val submitter = Observer[ForgotPasswordPageState] { state =>
    // check state error
    if (state.hasErrors) {
      // if error - show
      stateVar.update(_.copy(showStatus = true))
    } else {
      dom.console.log(s"Current state is: $state, trigger backend call")
      // no errors - trigger backend
      useBackend(_.userEndpoints.forgotPasswordEndpoint(ForgotPasswordRequest(state.email)))
        .map { user =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Right("Check your email."))
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
    button(
      `type` := "button",
      "Recover Password",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    ),
    div(
      cls := "auth-link",
      Anchors.renderNavLink("Have a password recovery token?", "/recover")
    )
  )
}

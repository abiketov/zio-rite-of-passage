package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.core.Session
import org.scalajs.dom.html.Element
import org.scalajs.dom

case class LogoutFormPage() extends FormState {

  override def showStatus: Boolean = false

  override def mayBeSuccess: Option[String] = None

  override def errorList: List[Option[String]] = List()
}

object LogoutPage extends FormPage[LogoutFormPage]("Logout") {

  override def basicState = LogoutFormPage()

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(
    div(
      onMountCallback(_ => Session.clearUserState()),
      cls := "logout-status",
      "You have been successfully logged out."
    )
  )
}

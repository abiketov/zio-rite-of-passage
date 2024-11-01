package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.api.L.{div, *, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.http.endpoints.UserEndpoints
import com.rockthejvm.reviewboard.http.requests.LoginRequest
import frontroute.*
import org.scalajs.dom
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*

trait FormState {

  def showStatus: Boolean
  def mayBeSuccess: Option[String]
  def errorList: List[Option[String]]

  def mayBeError = errorList.find(_.isDefined).flatten.filter(_ => showStatus)
  def hasErrors  = errorList.exists(_.isDefined)
  def mayBeStatus: Option[Either[String, String]] =
    mayBeError.map(Left(_)).orElse(mayBeSuccess.map(Right(_))).filter(_ => showStatus)
}

abstract class FormPage[S <: FormState](title: String) {

  val stateVar: Var[S]

  def renderChildren(): List[ReactiveHtmlElement[dom.html.Element]]

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
          div(cls := "top-section", h1(span(title))),
          children <-- stateVar.signal
            .map(_.mayBeStatus)
            .map(renserStatus)
            .map(_.toList),
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

  def renserStatus(status: Option[Either[String, String]]) =
    status.map {
      case Left(error) =>
        div(
          cls := "page-status-errors",
          error
        )
      case Right(message) =>
        div(
          cls := "page-status-success",
          message
        )
    }

  def renderInput(
      name: String,
      uid: String,
      kind: String,
      isRequired: Boolean,
      placeHolder: String,
      updateFn: (S, String) => S
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
}

package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.StringAsIsCodec
import com.rockthejvm.reviewboard.common.Constants
import org.scalajs.dom
import frontroute.*

import scala.scalajs.js
import scala.scalajs.js.annotation.*

object Header {

  def apply() = {
    div(
      cls := "container-fluid p-0",
      div(
        cls := "jvm-nav",
        div(
          cls := "container",
          navTag(
            cls := "navbar navbar-expand-lg navbar-light JVM-nav",
            div(
              cls := "container",
              // Add logo
              renderLogo(),
              button(
                cls                                         := "navbar-toggler",
                `type`                                      := "button",
                htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
                htmlAttr("data-bs-target", StringAsIsCodec) := "#navbarNav",
                htmlAttr("aria-controls", StringAsIsCodec)  := "navbarNav",
                htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
                htmlAttr("aria-label", StringAsIsCodec)     := "Toggle navigation",
                span(cls := "navbar-toggler-icon")
              ),
              div(
                cls    := "collapse navbar-collapse",
                idAttr := "navbarNav",
                ul(
                  cls := "navbar-nav ms-auto menu align-center expanded text-center SMN_effect-3",
                  // Add children
                  renderNavLinks()
                )
              )
            )
          )
        )
      )
    )

  }

  private def renderLogo() = {
    a(
      href := "/",
      cls  := "nav-bar-brand",
      img(
        cls := "home-log",
        src := Constants.logoImage
      )
    )
  }

  private def renderNavLinks() = { // List of <li>
    List(
      renderNavLink("Companies", "/companies"),
      renderNavLink("Log in", "/login"),
      renderNavLink("Sign Up", "/signup")
    )
  }

  private def renderNavLink(text: String, location: String) = {

    li(
      cls := "nav-item",
      Anchors.renderNavLink(text, location = location, cssClass = "nav-link jvm-item")
    )
  }

}

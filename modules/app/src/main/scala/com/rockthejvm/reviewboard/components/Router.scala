package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.pages.*
import org.scalajs.dom
import frontroute.*

object Router {

  def apply() =
    mainTag(
      routes(
        div(
          cls := "container-fluid",
          // potential children
          (pathEnd | path("companies")) { // localhost:1234 or localhost:1234/ or localhost:1234/companies
            CompaniesPage()
          },
          path("login") {
            LoginPage()
          },
          path("signup") {
            SignupPage()
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}

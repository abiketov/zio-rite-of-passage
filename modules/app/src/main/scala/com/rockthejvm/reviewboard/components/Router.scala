package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.pages.*
import org.scalajs.dom
import frontroute.*

object Router {

  val externalUrlBus = EventBus[String]()

  def apply() =
    mainTag(
      onMountCallback(ctx =>
        externalUrlBus.events.foreach { url =>
          // dom.console.log(s"url:$url")
          dom.window.location.href = url
        }(ctx.owner)
      ),
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
          path("profile") {
            ProfilePage()
          },
          path("signup") {
            SignupPage()
          },
          path("changepassword") {
            ChangePasswordPage()
          },
          path("logout") {
            LogoutPage()
          },
          path("forgot") {
            ForgotPasswordPage()
          },
          path("recover") {
            RecoverPasswordPage()
          },
          path("post") {
            CreateCompanyPage()
          },
          path("company" / long) { companyId =>
            CompanyPage(companyId)
          },
          noneMatched {
            NotFoundPage()
          }
        )
      )
    )
}

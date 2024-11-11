package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.components.*
import com.rockthejvm.reviewboard.core.BackendClient
import com.rockthejvm.reviewboard.domain.data.Company
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*
import com.rockthejvm.reviewboard.core.ZJS.*

object CompanyComponent {

  def renderCompanyPicture(company: Company) = {
    img(
      cls := "img-fluid",
      src := company.image.getOrElse(Constants.companyLogoImage),
      alt := company.name
    )
  }

  def renderDetail(icon: String, value: String) = {
    div(
      cls := "company-detail",
      i(cls := s"fa fa-$icon company-detail-icon"),
      p(
        cls := "company-detail-value",
        value
      )
    )
  }

  def fullLocationString(company: Company): String = {

    (company.location, company.country) match {
      case (Some(l), Some(c)) => s"$l, $c"
      case (None, Some(c))    => c
      case (Some(l), None)    => l
      case (None, None)       => "N/A"
    }
  }

  def renderOverview(company: Company) = {
    div(
      cls := "company-summary",
      renderDetail("location-dot", fullLocationString(company)),
      renderDetail("tags", company.tags.mkString(","))
    )
  }
}

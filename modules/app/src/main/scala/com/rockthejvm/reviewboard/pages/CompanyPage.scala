package com.rockthejvm.reviewboard.pages

import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.components.*
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.DomApi
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.core.*
import com.rockthejvm.reviewboard.core.BackendClient
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.pages.CompanyPage.Status.*
import zio.*
import org.scalajs.dom
import java.time.Instant

object CompanyPage {

  val dummyReviews = List(
    Review(
      1,
      1,
      1L,
      5,
      5,
      5,
      5,
      5,
      "This is a pretty good company. They write Scala and that's great",
      Instant.now(),
      Instant.now()
    ),
    Review(
      1,
      1,
      1L,
      3,
      4,
      3,
      4,
      4,
      "Pretty average. Not sure what to think about it. But here's some Markdown: _italics_, **bold**, ~strikethrough~.",
      Instant.now(),
      Instant.now()
    ),
    Review(
      1,
      1,
      1L,
      1,
      1,
      1,
      1,
      1,
      "Hate it with a passion.",
      Instant.now(),
      Instant.now()
    )
  )

  // the render function

  enum Status {
    case LOADING
    case NOT_FOUND
    case OK(company: Company)
  }

  // reactive variable
  private val fetchCompanyBus     = EventBus[Option[Company]]()
  private val addReviewCardActive = Var[Boolean](false)
  val triggerRefreshBus           = EventBus[Unit]()

  def refreshReviewList(companyId: Long): EventStream[List[Review]] = {
    useBackend(_.reviewEndpoints.getByCompanyIdEndpoint(companyId)).toEventStream
      .mergeWith(
        // call to get updated list of reviews on triggerRefreshBus event
        triggerRefreshBus.events.flatMap(_ =>
          useBackend(_.reviewEndpoints.getByCompanyIdEndpoint(companyId)).toEventStream
        )
      )
  }

  def reviewsSignal(companyId: Long): Signal[List[Review]] = fetchCompanyBus.events
    .flatMap {
      case None          => EventStream.empty // new EventStream[List[Review]]
      case Some(company) => refreshReviewList(companyId)
    }
    .scanLeft(List[Review]())((_, list) => list)

  val status = fetchCompanyBus.events.scanLeft(Status.LOADING)((_, maybeCompany) =>
    maybeCompany match {
      case None          => Status.NOT_FOUND
      case Some(company) => Status.OK(company)
    }
  )

  def apply(companyId: Long) = {
    div(
      cls := "container-fluid the-rock",
      onMountCallback(_ =>
        useBackend(_.companyEndpoints.getByIdEndpoint(companyId.toString)).emitTo(fetchCompanyBus)
      ),
      children <-- status.map {
        case LOADING     => List(div("Loading..."))
        case NOT_FOUND   => List(div("Not Found"))
        case OK(company) => renderCompany(company, reviewsSignal(companyId))
      }
    )
  }

  def renderCompany(company: Company, reviewSignal: Signal[List[Review]]) = List(
    div(
      cls := "row jvm-companies-details-top-card",
      div(
        cls := "col-md-12 p-0",
        div(
          cls := "jvm-companies-details-card-profile-img",
          CompanyComponent.renderCompanyPicture(company)
        ),
        div(
          cls := "jvm-companies-details-card-profile-title",
          h1(company.name),
          div(
            cls := "jvm-companies-details-card-profile-company-details-company-and-location",
            CompanyComponent.renderOverview(company)
          )
        ),
        child <-- Session.userState.signal.map(maybeUser =>
          maybeRenderUserAction(maybeUser, reviewSignal)
        )
      )
    ),
    div(
      cls := "container-fluid",
      renderCompanySummary,
      children <-- addReviewCardActive.signal
        .map(active =>
          Option.when(active)(
            AddReviewCard(
              company.id,
              onDisable = () => addReviewCardActive.set(false),
              triggerRefreshBus
            )()
          )
        )
        .map(_.toList),
      children <-- reviewSignal.map(_.map(renderReview)),
      div(
        cls := "container",
        div(
          cls := "rok-last",
          div(
            cls := "row invite-row",
            div(
              cls := "col-md-6 col-sm-6 col-6",
              span(
                cls := "rock-apply",
                p("Do you represent this company?"),
                p("Invite people to leave reviews.")
              )
            ),
            div(
              cls := "col-md-6 col-sm-6 col-6",
              a(
                href   := company.url,
                target := "blank",
                button(`type` := "button", cls := "rock-action-btn", "Invite people")
              )
            )
          )
        )
      )
    )
  )

  def maybeRenderUserAction(maybeUser: Option[UserToken], reviewsSignal: Signal[List[Review]]) = {

    maybeUser match {
      case None =>
        div(
          cls := "jvm-companies-details-card-apply-now-btn",
          "You must be logged in to post a review"
        )
      case Some(user) =>
        div(
          cls := "jvm-companies-details-card-apply-now-btn",
          child <-- reviewsSignal
            .map(list =>
              list.find { review =>
                org.scalajs.dom.console.log(s"review user id:${review.userId}")
                review.userId == 100 // user.id
              }
            ) // TODO: add userId
            .map {
              case None =>
                org.scalajs.dom.console.log(s"user id:${user.id}")
                button(
                  `type` := "button",
                  cls    := "btn btn-warning",
                  "Add a review",
                  disabled <-- addReviewCardActive.signal,
                  onClick.mapTo(true) --> addReviewCardActive.writer
                )
              case Some(_) =>
                div("You already have posted a review")
            }
        )
    }
  }

  def renderCompanySummary =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description",
          "TODO company summary"
        )
      )
    )

  def renderReview(review: Review) =
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        // add a highlight if this is "your" review
        cls.toggle("review-highlighted") <-- Session.userState.signal.map(maybeUser =>
          maybeUser.map(_.id) == Option(review).map(_.userId)
        ),
        div(
          cls := "company-description",
          div(
            cls := "review-summary",
            renderStaticReviewDetail("Would Recommend", review.wouldRecommend),
            renderStaticReviewDetail("Management", review.management),
            renderStaticReviewDetail("Culture", review.culture),
            renderStaticReviewDetail("Salary", review.salary),
            renderStaticReviewDetail("Benefits", review.benefits)
          ),
          // parse this Markdown
          injectMarkdown(review),
          div(cls := "review-posted", s"Posted ${Time.unixToHr(review.created.toEpochMilli)}"),
          child.maybe <-- Session.userState.signal
            .map(_.filter(_.id == review.userId))
            .map(_.map(_ => div(cls := "review-posted", "Your review")))
        )
      )
    )

  def injectMarkdown(review: Review) = {
    div(
      cls := "review-content",
      DomApi
        .unsafeParseHtmlStringIntoNodeArray(Markdown.toHtml(review.review))
        .map {
          case t: dom.Text         => span(t.data)
          case e: dom.html.Element => foreignHtmlElement(e)
          case _                   => emptyNode
        }
    )
  }

  def renderStaticReviewDetail(detail: String, score: Int) =
    div(
      cls := "review-detail",
      span(cls := "review-detail-name", s"$detail: "),
      (1 to score).toList.map(_ =>
        svg.svg(
          svg.cls     := "review-rating",
          svg.viewBox := "0 0 32 32",
          svg.path(
            svg.d := "m15.1 1.58-4.13 8.88-9.86 1.27a1 1 0 0 0-.54 1.74l7.3 6.57-1.97 9.85a1 1 0 0 0 1.48 1.06l8.62-5 8.63 5a1 1 0 0 0 1.48-1.06l-1.97-9.85 7.3-6.57a1 1 0 0 0-.55-1.73l-9.86-1.28-4.12-8.88a1 1 0 0 0-1.82 0z"
          )
        )
      )
    )
}

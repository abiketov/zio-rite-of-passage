package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.http.requests.CreateReviewRequest
import org.scalajs.dom
import zio.*

class AddReviewCard(companyId: Long, onDisable: () => Unit, triggerBus: EventBus[Unit]) {

  case class State(
      review: Review = Review.empty(companyId),
      showErrors: Boolean = false,
      upstreamError: Option[String] = None
  )

  val stateVar = Var(State())

  val submitter = Observer[State] { state =>
    // check state error
    if (state.showErrors) {
      // if error - show
      stateVar.update(_.copy(showErrors = true))
    } else {
      dom.console.log(s"Current state is: $state, trigger backend call")
      // no errors - trigger backend
      useBackend(
        _.reviewEndpoints.createEndpoint(CreateReviewRequest.fromReview(state.review))
      )
        .map { resp => onDisable() }
        .tapError { e =>
          ZIO.succeed(
            stateVar.update(_.copy(showErrors = true, upstreamError = Some(e.getMessage)))
          )
        }
        .emitTo(triggerBus)
    }

    // if backend returns error - shoe error
    // if success - set user token, navigate to home page
    // dom.console.log(s"Current state is: $state")

  }

  def apply() = {
    div(
      cls := "container",
      div(
        cls := "markdown-body overview-section",
        div(
          cls := "company-description add-review",
          div(
            // score dropdowns
            div(
              cls := "add-review-scores",
              renderScoreSelectorDropdown("Would recommend", (r, v) => r.copy(wouldRecommend = v)),
              renderScoreSelectorDropdown("Management", (r, v) => r.copy(management = v)),
              renderScoreSelectorDropdown("Culture", (r, v) => r.copy(culture = v)),
              renderScoreSelectorDropdown("Salary", (r, v) => r.copy(salary = v)),
              renderScoreSelectorDropdown("Benefits", (r, v) => r.copy(benefits = v))

              // TODO do the same for all score fields
            ),
            // text area for the text review
            div(
              cls := "add-review-text",
              label(forId := "add-review-text", "Your review - supports Markdown"),
              textArea(
                idAttr      := "add-review-text",
                cls         := "add-review-text-input",
                placeholder := "Write your review here",
                onInput.mapToValue --> stateVar.updater { (s: State, value: String) =>
                  s.copy(review = s.review.copy(review = value))
                }
              )
            ),
            button(
              `type` := "button",
              cls    := "btn btn-warning rock-action-btn",
              "Post review",
              onClick.preventDefault.mapTo(stateVar.now()) --> submitter
              // TODO post the review on this button
            ),
            div(
              paddingRight := "20em",
              button(
                `type` := "button",
                cls    := "btn btn-warning rock-action-btn",
                "Cancel",
                onClick --> (_ => onDisable())
                // TODO post the review on this button
              )
            ),
            // TODO show potential errors here
            children <-- stateVar.signal
              .map(s => s.upstreamError.filter(_ => s.showErrors))
              .map(maybeRenderError)
              .map(_.toList)
          )
        )
      )
    )
  }

  private def maybeRenderError(maybeError: Option[String]) = {
    maybeError.map { message =>
      div(
        cls := "page-status-error",
        message
      )
    }
  }

  private def renderScoreSelectorDropdown(name: String, updateFn: (Review, Int) => Review) = {
    val selectorId = name.split(" ").map(_.toLowerCase).mkString("-")
    div(
      cls := "add-review-score",
      label(forId := selectorId, s"$name:"),
      select(
        idAttr := selectorId,
        (1 to 5).reverse.map { v =>
          option(
            v.toString
          )

        }
      ),
      onInput.mapToValue --> stateVar.updater { (state: State, value: String) =>
        state.copy(review = updateFn(state.review, value.toInt))
      }
    )
  }
}

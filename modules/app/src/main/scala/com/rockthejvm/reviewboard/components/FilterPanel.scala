package com.rockthejvm.reviewboard.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.codecs.StringAsIsCodec
import com.rockthejvm.reviewboard.common.Constants
import com.rockthejvm.reviewboard.components.*
import com.rockthejvm.reviewboard.domain.data.{Company, CompanyFilter}
import com.rockthejvm.reviewboard.http.endpoints.CompanyEndpoints
import sttp.client3.*
import sttp.client3.impl.zio.FetchZioBackend
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.*
import com.rockthejvm.reviewboard.core.ZJS.*
import sttp.capabilities
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.model.Uri
import sttp.tapir.Endpoint

/** Populate proper filter data with values from database
  *   - expose backend API to retrieve the unique values for filtering
  *   - fetch those values to populate the panel Update filter panel when user interacts with it
  *     When clicking "apply filters" we should retrieve those companies
  *   - make the backend work
  *   - refetch companies when user clicks the filter
  */

class FilterPanel {

  case class CheckValueEvent(group: String, value: String, checked: Boolean)

  private val LOCATIONS  = "Locations"
  private val COUNTRIES  = "Countries"
  private val INDUSTRIES = "Industries"
  private val TAGS       = "Tags"

  // val possibleFilter: Var[CompanyFilter] = Var[CompanyFilter](CompanyFilter.empty)
  private val possibleFilter: EventBus[CompanyFilter] = EventBus[CompanyFilter]()
  private val checkEvents: EventBus[CheckValueEvent]  = EventBus[CheckValueEvent]()

  private val clicks: EventBus[Unit] = EventBus[Unit]() // clicks on apply button
  // emits true or false depending on whether to show "apply" or not
  private val dirty = clicks.events.mapTo(false).mergeWith(checkEvents.events.mapTo(true))

  // val checks = Map[String, Set[String]]
  private val state: Signal[CompanyFilter] = checkEvents.events
    .scanLeft(Map[String, Set[String]]()) { (currentMap, event) =>
      event match {
        case CheckValueEvent(groupName, value, checked) =>
          if (checked) currentMap + (groupName -> (currentMap.getOrElse(groupName, Set()) + value))
          else currentMap + (groupName         -> (currentMap.getOrElse(groupName, Set()) - value))
      }
    } // Signal[Map[String,Set[String]]]
    .map { checkMap =>
      CompanyFilter(
        locations = checkMap.getOrElse(LOCATIONS, Set()).toList,
        countries = checkMap.getOrElse(COUNTRIES, Set()).toList,
        industries = checkMap.getOrElse(INDUSTRIES, Set()).toList,
        tags = checkMap.getOrElse(TAGS, Set()).toList
      )

    }

  def performBackendCall(): Unit = {
    val companyEndpoints = new CompanyEndpoints {}
    // run ZIO effect
    val filterZIO = useBackend(_.companyEndpoints.allFiltersEndpoint(()))
    // filterZIO.map(f => possibleFilter.set(f)).runJs()
    filterZIO.emitTo(possibleFilter) /// .runJs()
  }

  // informs company page to refresh
  val triggerFilters: EventStream[CompanyFilter] = clicks.events.withCurrentValueOf(state)

  def apply() =
    div(
      onMountCallback(_ => performBackendCall()),
      child.text <-- triggerFilters.map(_.toString),
      cls    := "accordion accordion-flush",
      idAttr := "accordionFlushExample",
      div(
        cls := "accordion-item",
        h2(
          cls    := "accordion-header",
          idAttr := "flush-headingOne",
          button(
            cls                                         := "accordion-button",
            idAttr                                      := "accordion-search-filter",
            `type`                                      := "button",
            htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
            htmlAttr("data-bs-target", StringAsIsCodec) := "#flush-collapseOne",
            htmlAttr("aria-expanded", StringAsIsCodec)  := "true",
            htmlAttr("aria-controls", StringAsIsCodec)  := "flush-collapseOne",
            div(
              cls := "jvm-recent-companies-accordion-body-heading",
              h3(
                span("Search"),
                " Filters"
              )
            )
          )
        ),
        div(
          cls                                          := "accordion-collapse collapse show",
          idAttr                                       := "flush-collapseOne",
          htmlAttr("aria-labelledby", StringAsIsCodec) := "flush-headingOne",
          htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionFlushExample",
          div(
            cls := "accordion-body p-0",
            renderFilterOptions(LOCATIONS, _.locations),
            renderFilterOptions(COUNTRIES, _.countries),
            renderFilterOptions(INDUSTRIES, _.industries),
            renderFilterOptions(TAGS, _.tags),
            renderApplyButton()
          )
        )
      )
    )

  def renderFilterOptions(groupName: String, optionsFn: CompanyFilter => List[String]) =
    div(
      cls := "accordion-item",
      h2(
        cls    := "accordion-header",
        idAttr := s"heading$groupName",
        button(
          cls                                         := "accordion-button collapsed",
          `type`                                      := "button",
          htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
          htmlAttr("data-bs-target", StringAsIsCodec) := s"#collapse$groupName",
          htmlAttr("aria-expanded", StringAsIsCodec)  := "false",
          htmlAttr("aria-controls", StringAsIsCodec)  := s"collapse$groupName",
          groupName
        )
      ),
      div(
        cls                                          := "accordion-collapse collapse",
        idAttr                                       := s"collapse$groupName",
        htmlAttr("aria-labelledby", StringAsIsCodec) := "headingOne",
        htmlAttr("data-bs-parent", StringAsIsCodec)  := "#accordionExample",
        div(
          cls := "accordion-body",
          div(
            cls := "mb-3",
            // stateful variables Signal and Var
            children <-- possibleFilter.events
              .toSignal(CompanyFilter.empty)
              .map(filter => optionsFn(filter).map(value => renderCheckBox(groupName, value)))
          )
        )
      )
    )

  private def renderCheckBox(groupName: String, value: String) = {
    div(
      cls := "form-check",
      label(
        cls   := "form-check-label",
        forId := s"filter-$groupName-$value",
        value
      ),
      input(
        cls    := "form-check-input",
        `type` := "checkbox",
        idAttr := s"filter-$groupName-$value",
        onChange.mapToChecked.map(checked =>
          CheckValueEvent(groupName, value, checked)
        ) --> checkEvents
      )
    )
  }

  private def renderApplyButton() = {
    div(
      cls := "jvm-accordion-search-btn",
      button(
        disabled <-- dirty.toSignal(false).map(v => !v),
        onClick.mapTo(()) --> clicks,
        cls    := "btn btn-primary",
        `type` := "button",
        "Apply Filters"
      )
    )
  }

}

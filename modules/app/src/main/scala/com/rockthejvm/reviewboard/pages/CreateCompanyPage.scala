package com.rockthejvm.reviewboard.pages

import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.common.Constants
import org.scalajs.dom.html.Element
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import com.rockthejvm.reviewboard.core.ZJS.*
import com.rockthejvm.reviewboard.http.requests.*
import org.scalajs.dom
import org.scalajs.dom.html
import zio.*
import org.scalajs.dom.*

case class CreateCompanyPageState(
    name: String = "",
    url: String = "",
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = List.empty,
    upstreamStatus: Option[Either[String, String]] = None,
    override val showStatus: Boolean = false
) extends FormState {

  override def mayBeSuccess: Option[String] = upstreamStatus.flatMap((_.toOption))

  override def errorList: List[Option[String]] = List(
    Option.when(!url.matches(Constants.urlRegex))("Token can't be empty"),
    Option.when(name.isEmpty)("Company name can't be empty")
  ) ++ upstreamStatus.map(_.left.toOption).toList

  def toRequest: CreateCompanyRequest = {
    CreateCompanyRequest(
      name,
      url,
      location,
      country,
      industry,
      image,
      Some(tags)
    )

  }
}

object CreateCompanyPage extends FormPage[CreateCompanyPageState]("Create Company") {
  override def basicState: CreateCompanyPageState = CreateCompanyPageState()

  val submitter = Observer[CreateCompanyPageState] { state =>
    // check state error
    if (state.hasErrors) {
      // if error - show
      stateVar.update(_.copy(showStatus = true))
    } else {
      dom.console.log(s"Current state is: $state, trigger backend call")
      // no errors - trigger backend
      useBackend(
        _.companyEndpoints.createEndpoint(state.toRequest)
      )
        .map { company =>
          stateVar.update(
            _.copy(
              showStatus = true,
              upstreamStatus = Some(Right("Company was created. Check it in companies list"))
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

    // if backend returns error - shoe error
    // if success - set user token, navigate to home page
    dom.console.log(s"Current state is: $state")

  }

  val fileUploader = (files: List[File]) => {
    // take the file

    val maybeFile = files.headOption.filter(_.size > 0)
    maybeFile.foreach { file =>
      // make a FileLoader
      // register onload callback => when it's done, populate state with reader.result
      // trigger the read
      val reader = new FileReader
      reader.onload = _ => {
        // 256 x 256
        // draw the picture into 256x256 canvas
        // make a fake img tag (not rendered)
        val fakeImage = document.createElement("img").asInstanceOf[HTMLImageElement]
        fakeImage.addEventListener(
          "load",
          _ => {
            // inside, make a canvas of at most 256x256
            // render the original image into that canvas
            // set the state to the text repr of img2
            val canvas          = document.createElement("canvas").asInstanceOf[HTMLCanvasElement]
            val (width, height) = computeDimensions(fakeImage.width, fakeImage.height)
            canvas.width = width
            canvas.height = height
            val context = canvas.getContext("2d").asInstanceOf[CanvasRenderingContext2D]
            context.drawImage(fakeImage, 0, 0, width, height)
            stateVar.update(_.copy(image = Some(canvas.toDataURL(file.`type`))))
          }
        )
        fakeImage.src = reader.result.toString
      }
      reader.readAsDataURL(file)
    }
  }

  override def renderChildren(): List[ReactiveHtmlElement[Element]] = List(
    renderInput(
      "Company name",
      "company-name",
      "text",
      true,
      "Company name",
      (s, p) => s.copy(name = p, showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Company url",
      "company-url",
      "text",
      true,
      "https://somecompany.com",
      (s, p) => s.copy(url = p, showStatus = false, upstreamStatus = None)
    ),
    renderLogoUpload("Company Logo", "company-logo", isRequired = false),
    img(
      src <-- stateVar.signal.map(_.image.getOrElse(""))
    ),
    renderInput(
      "Company location",
      "company-location",
      "text",
      false,
      "Company location",
      (s, p) => s.copy(location = Some(p), showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Company country",
      "company-country",
      "text",
      false,
      "Company country",
      (s, p) => s.copy(country = Some(p), showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Company industry",
      "company-industry",
      "text",
      false,
      "Company industry",
      (s, p) => s.copy(industry = Some(p), showStatus = false, upstreamStatus = None)
    ),
    renderInput(
      "Tags - (separate by ' , ')",
      "company-tags",
      "text",
      false,
      "scala, java, kafka",
      (s, p) =>
        s.copy(tags = p.split(",").map(_.trim).toList, showStatus = false, upstreamStatus = None)
    ),
    button(
      `type` := "button",
      "Create Company",
      onClick.preventDefault.mapTo(stateVar.now()) --> submitter
    )
  )

  def renderLogoUpload(name: String, uid: String, isRequired: Boolean = false) = {
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
            `type` := "file",
            cls    := "form-control",
            idAttr := uid,
            accept := "image/*",
            onChange.mapToFiles --> fileUploader
          )
        )
      )
    )
  }

  private def computeDimensions(width: Int, height: Int): (Int, Int) = {
    if (width > height) {
      val ratio = width * 1.0 / 256
      val newW  = width / ratio
      val newH  = height / ratio
      (newW.toInt, newH.toInt)
    } else {
      val (newH, newW) = computeDimensions(height, width)
      (newW, newH)
    }
  }
}

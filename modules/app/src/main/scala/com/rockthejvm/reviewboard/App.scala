package com.rockthejvm.reviewboard

import com.raquo.airstream.ownership.OneTimeOwner
import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.components.{Header, Router}
import frontroute.LinkHandler
import org.scalajs.dom

import scala.util.Try

object App {

  val app = div(
    Header(),
    Router()
  ).amend(LinkHandler.bind)

  def main(args: Array[String]): Unit = {

    val containerNode = dom.document.querySelector("#app")
    render(
      container = containerNode,
      rootNode = app
    )
  }
}

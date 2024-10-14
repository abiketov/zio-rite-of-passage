package com.rockthejvm.reviewboard

import com.raquo.airstream.ownership.OneTimeOwner
import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

import scala.util.Try

object DemoApp {

  def main(args: Array[String]): Unit = {

    val containerNode = dom.document.querySelector("#app")
    render(
      container = containerNode,
      rootNode = Tutorial.clicksVar
    )
  }
}

object Tutorial {

  val staticContent =
    div(
      styleAttr := "color:red",
      p("Rock the JVM with Laminar!"),
      p("Just do it!")
    )

  val ticks = EventStream.periodic(1000)
  // subscription - AirStream
  val subscription = ticks.addObserver(new Observer[Int] {
    override def onNext(nextValue: Int): Unit = dom.console.log(s"sTicks:$nextValue")

    override def onError(err: Throwable): Unit = ()

    override def onTry(nextValue: Try[Int]): Unit = ()
  })(new OneTimeOwner(() => ()))

  scala.scalajs.js.timers.setTimeout(10000)(subscription.kill())

  val timeUpdated = div(
    span("Time since loaded: "),
    child <-- ticks.map(number => s"$number seconds")
  )
  // Event streams

  // EventBus like EventStreams but you can push new elements to the stream
  val clickEvents = EventBus[Int]()
  val clickUpdated = div(
    span("Clicks since loaded: "),
    child <-- clickEvents.events.scanLeft(0)(_ + _).map(number => s"$number clicks"),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.map(_ => 1) --> clickEvents,
      "Add click"
    )
  )

  // Signal - similar to EventStreams but they have a current value. We can model state with Signals
  // can be inspected for a current state if Laminar/AirStream knows that it has an owner
  val countSignal =
    clickEvents.events.scanLeft(0)(_ + _).observe(new OneTimeOwner(() => ()))
  val queryEvents = EventBus[Unit]()

  val clickQueried = div(
    span("Clicks since loaded: "),
    child <-- queryEvents.events.map(_ => countSignal.now()),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.map(_ => 1) --> clickEvents,
      "Add click"
    ),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      onClick.map(_ => ()) --> queryEvents,
      "Refresh count"
    )
  )
  // Var - reactive variable
  val countVar = Var[Int](0)
  val clicksVar = div(
    span("Clicks so far:"),
    child <-- countVar.signal.map(_.toString),
    button(
      `type`    := "button",
      styleAttr := "display: block",
      // onClick --> countVar.updater((current, event) => current + 1),
      // onClick --> countVar.writer.contramap(event => countVar.now() + 1),
      onClick --> (_ => countVar.set(countVar.now() + 1)),
      "Add Var click"
    )
  )

  /** no state | with state
    * ------------------------------------------------------------------- read EventStream | Signal
    * ------------------------------------------------------------------- write EventBus | Var
    */

}

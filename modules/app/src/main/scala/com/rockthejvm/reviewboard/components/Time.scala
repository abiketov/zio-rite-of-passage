package com.rockthejvm.reviewboard.components

// import moment.js functionality

import scala.scalajs.*
import scala.scalajs.js.*
import scala.scalajs.js.annotation.*

// require moment.js in scala
/** for example moment.something(Int): String implement same function in MomentLib object
  */

@js.native
@JSGlobal
class Moment extends js.Object {

  def format(): String  = js.native
  def fromNow(): String = js.native
}

@js.native
@JSImport("moment", JSImport.Default)
object MomentLib extends js.Object {

  def unix(millis: Long): Moment = js.native

}

object Time {

  def unixToHr(millis: Long) = MomentLib.unix(millis / 1000).fromNow()
}

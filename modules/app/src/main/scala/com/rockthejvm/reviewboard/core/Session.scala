package com.rockthejvm.reviewboard.core

import com.raquo.laminar.api.L.{*, given}
import com.rockthejvm.reviewboard.domain.data.UserToken
import scala.scalajs.js.*

object Session {

  private val stateName                 = "userState"
  val userState: Var[Option[UserToken]] = Var(Option.empty)

  def isActive = {
    loadUserState()
    userState.now().nonEmpty
  }

  def setUserState(token: UserToken): Unit = {
    userState.set(Option(token))
    Storage.set("userState", token)
  }

  def loadUserState(): Unit = {

    Storage
      .get[UserToken](stateName)
      .filter(token => token.expires * 1000 < new Date().getTime())
      .foreach(token => Storage.remove(stateName))
    val currentToken = Storage.get[UserToken](stateName)
    if (userState.now() != currentToken)
      userState.set(Storage.get[UserToken](stateName))
  }

  def clearUserState(): Unit = {
    Storage.remove(stateName)
    userState.set(Option.empty)
  }

  def getUserState: Option[UserToken] = {
    loadUserState()
    userState.now()
  }
}

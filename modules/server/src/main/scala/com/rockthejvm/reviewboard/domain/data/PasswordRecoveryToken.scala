package com.rockthejvm.reviewboard.domain.data

case class PasswordRecoveryToken(email: String, token: String, expiration: Long)

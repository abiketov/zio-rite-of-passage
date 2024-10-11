package com.rockthejvm.reviewboard.http.requests

import zio.json.{DeriveJsonCodec, JsonCodec}

case class RegisterUserAccount(email: String, password: String) derives JsonCodec

case class DeleteAccountRequest(email: String, password: String) derives JsonCodec

case class LoginRequest(email: String, password: String) derives JsonCodec

//object RegisterUserAccount {
//  given codec: JsonCodec[RegisterUserAccount] = DeriveJsonCodec.gen[RegisterUserAccount]
//}

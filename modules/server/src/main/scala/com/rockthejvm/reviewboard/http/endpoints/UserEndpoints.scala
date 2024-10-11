package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.http.requests.{
  DeleteAccountRequest,
  ForgotPasswordRequest,
  LoginRequest,
  RecoverPasswordRequest,
  RegisterUserAccount,
  UpdatePasswordRequest
}
import com.rockthejvm.reviewboard.http.responses.UserResponse

trait UserEndpoints extends BaseEndPoint {

  val createUserEndpoint =
    baseEndPoint
      .tag("Users")
      .name("register")
      .description("Register a user account with username and password")
      .in("users")
      .post
      .in(jsonBody[RegisterUserAccount])
      .out(jsonBody[UserResponse])

  // PUT /users/password {email, oldPassword, newPassword} -> {email}
  // TODO - should be an authorized endpoint (JWT)
  val updatePasswordEndpoint =
    secureBaseEndpoint
      .tag("Users")
      .name("update password")
      .description("update user password")
      .in("users" / "password")
      .put
      .in(jsonBody[UpdatePasswordRequest])
      .out(jsonBody[UserResponse])

  // DELETE /users {email, password} -> {email}
  // TODO - should be an authorized endpoint (JWT)
  val deleteUserEndpoint =
    secureBaseEndpoint
      .tag("delete")
      .name("delete")
      .description("")
      .in("users")
      .delete
      .in(jsonBody[DeleteAccountRequest])
      .out(jsonBody[UserResponse])

  // POST /users/login {email, password} -> {email, accessToken, expiration}
  val loginEndpoint =
    baseEndPoint
      .tag("Users")
      .name("login")
      .description("Login and generate JWT token")
      .in("users" / "login")
      .post
      .in(jsonBody[LoginRequest])
      .out(jsonBody[UserToken])

  // forgot password flow
  // POST /users/forgot {email} - 200 OK

  val forgotPasswordEndpoint = baseEndPoint
    .tag("Users")
    .name("forgot password")
    .description("Trigger email for password recovery")
    .in("users" / "forgot")
    .post
    .in(jsonBody[ForgotPasswordRequest])

  // recover password
  // POST /users/recover {email, token, newPassword}

  val recoverPasswordEndpoint = baseEndPoint
    .tag("Users")
    .name("recover password")
    .description("Set password based on OTP")
    .in("users" / "recover")
    .post
    .in(jsonBody[RecoverPasswordRequest])

}

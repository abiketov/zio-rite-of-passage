package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

case class UpdatePasswordRequest(email: String, oldPassword: String, newPassword: String)
    derives JsonCodec

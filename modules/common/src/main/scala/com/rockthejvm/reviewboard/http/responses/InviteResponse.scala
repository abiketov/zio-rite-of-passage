package com.rockthejvm.reviewboard.http.responses

import zio.json.JsonCodec

case class InviteResponse(status: String, nInvites: Int) derives JsonCodec

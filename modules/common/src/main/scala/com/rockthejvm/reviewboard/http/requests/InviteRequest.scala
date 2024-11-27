package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

case class InviteRequest(companyId: Long, emails: List[String]) derives JsonCodec

package com.rockthejvm.reviewboard.http.requests

import zio.json.JsonCodec

case class InvitePackRequest(companyId: Long) derives JsonCodec

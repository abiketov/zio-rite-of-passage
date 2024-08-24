package com.rockthejvm.reviewboard.http.endpoints

import sttp.tapir.*

trait HealthEndpoint extends BaseEndPoint {

  val healthEndpoint = baseEndPoint
    .tag("health")
    .name("health")
    .description("health check")
    .get
    .in("health")
    .out(plainBody[String])

  val errorEndPoint = baseEndPoint
    .tag("health")
    .name("error health")
    .description("health check - should fail")
    .get
    .in("health" / "error")
    .out(plainBody[String])
}

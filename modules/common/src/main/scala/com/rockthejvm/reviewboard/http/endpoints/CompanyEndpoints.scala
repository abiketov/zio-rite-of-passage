package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.*
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest

trait CompanyEndpoints extends BaseEndPoint {

  val createEndpoint = secureBaseEndpoint
    .tag("companies")
    .name("create")
    .description("Create a listing for a company")
    .in("companies")
    .post
    .in(jsonBody[CreateCompanyRequest])
    .out(jsonBody[Company])

  val getAllEndpoint = baseEndPoint
    .tag("companies")
    .name("getAll")
    .description("Get all companies listing")
    .in("companies")
    .get
    .out(jsonBody[List[Company]])

  val getByIdEndpoint = baseEndPoint
    .tag("companies")
    .name("getById")
    .description("Get company by Id")
    .in("companies" / path[String]("id"))
    .get
    .out(jsonBody[Option[Company]])

  val allFiltersEndpoint = baseEndPoint
    .tag("Companies")
    .name("allFilters")
    .description("Get all possible search filters")
    .in("companies" / "filters")
    .get
    .out(jsonBody[CompanyFilter])

  val searchEndpoint = baseEndPoint
    .tag("Companies")
    .name("search")
    .description("Get companies based on filters")
    .in("companies" / "search")
    .post
    .in(jsonBody[CompanyFilter])
    .out(jsonBody[List[Company]])
}

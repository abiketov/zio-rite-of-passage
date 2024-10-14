package com.rockthejvm.reviewboard.domain.data

import zio.json.JsonCodec
import zio.json.DeriveJsonCodec

final case class Company(
    id: Long,
    slug: String,
    name: String, // "My Company Inc" -> companies.rockthejvm.com/company/my-company-inc
    url: String,
    location: Option[String] = None,
    country: Option[String] = None,
    industry: Option[String] = None,
    image: Option[String] = None,
    tags: List[String] = List.empty
)

object Company {
  given codec: JsonCodec[Company] = DeriveJsonCodec.gen[Company]

  def makeSlug(name: String) = {
    name
      .replaceAll(" +", " ")
      .split(" ")
      .map(_.toLowerCase())
      .mkString("-")
  }

}

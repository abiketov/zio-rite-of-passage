package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.Company
import com.rockthejvm.reviewboard.http.requests.CreateCompanyRequest
import zio.*
import zio.test.*
import com.rockthejvm.reviewboard.syntax.*
import com.rockthejvm.reviewboard.repositories.*

object CompanyServiceSpec extends ZIOSpecDefault {

  val service = ZIO.serviceWithZIO[CompanyService]

  val stubRepoLayer = ZLayer.succeed(
    new CompanyRepository {

      val db = collection.mutable.Map[Long, Company]()

      override def create(company: Company): Task[Company] =
        ZIO.succeed {
          val nextId     = db.keys.maxOption.getOrElse(0L) + 1
          val newCompany = company.copy(id = nextId)
          db += (nextId -> newCompany)
          newCompany
        }

      override def update(id: Long, op: Company => Company): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db += (id -> op(company))
          company
        }

      override def delete(id: Long): Task[Company] =
        ZIO.attempt {
          val company = db(id)
          db -= id
          company
        }

      override def getById(id: Long): Task[Option[Company]] = ZIO.succeed(db.get(id))

      override def getBySlug(slug: String): Task[Option[Company]] =
        ZIO.succeed(db.values.find(_.slug == slug))

      override def get: Task[List[Company]] = ZIO.succeed(db.values.toList)
    }
  )

  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("Company service spec must")(
      test("create") {

        val companyZIO = service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))

        companyZIO.assert { company =>
          company.name == "Rock the JVM" &&
          company.url == "rockthejvm.com" &&
          company.slug == "rock-the-jvm"
        }
      },
      test("get by id") {
        // create company

        // fetch company by id
        val program = for {
          company    <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          companyOpt <- service(_.getById(company.id))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyOpt)) =>
            company.name == "Rock the JVM" &&
            company.url == "rockthejvm.com" &&
            company.slug == "rock-the-jvm" &&
            company == companyOpt

          case _ => false
        }
      },
      test("get by slug") {
        // create company

        // fetch company by id
        val program = for {
          company    <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          companyOpt <- service(_.getBySlug(company.slug))
        } yield (company, companyOpt)

        program.assert {
          case (company, Some(companyOpt)) =>
            company.name == "Rock the JVM" &&
            company.url == "rockthejvm.com" &&
            company.slug == "rock-the-jvm" &&
            company == companyOpt

          case _ => false
        }
      },
      test("get all") {
        val program = for {
          company1  <- service(_.create(CreateCompanyRequest("Rock the JVM", "rockthejvm.com")))
          company2  <- service(_.create(CreateCompanyRequest("Google", "google.com")))
          companies <- service(_.getAll)

        } yield (company1, company2, companies)

        program.assert {
          case (company1, company2, companies) =>
            companies.toSet == Set(company1, company2)
          case _ => false
        }

      }
    ).provide(CompanyServiceLive.layer, stubRepoLayer)
}

package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.Company
import com.rockthejvm.reviewboard.syntax.*
import zio.*
import zio.test.*

import java.sql.SQLException
import javax.sql.DataSource
object CompanyRepositorySpec extends ZIOSpecDefault with RepositorySpec {

  override val initScript: String = "sql/companies.sql"

  private val rtjvm = Company(1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com")

  private def genString() = {
    scala.util.Random.alphanumeric.take(8).mkString
  }
  private def genCompany(): Company =
    Company(-1L, genString(), genString(), genString())
  override def spec: Spec[TestEnvironment with Scope, Any] = suite("CompanyRepositorySpec")(
    test("create a company") {
      val program = for {
        repo    <- ZIO.service[CompanyRepository]
        company <- repo.create(rtjvm)
      } yield company

      program.assert {
        case Company(_, "rock-the-jvm", "Rock the JVM", "rockthejvm.com", _, _, _, _, _) => true
        case _                                                                           => false
      }

    },
    test("create a company duplicate returns error") {
      val program = for {
        repo  <- ZIO.service[CompanyRepository]
        _     <- repo.create(rtjvm)
        error <- repo.create(rtjvm).flip
      } yield error

      program.assert {
        case _: SQLException => true
        case _               => false
      }
    },
    test("getById and getBySlug") {
      val program = for {
        repo          <- ZIO.service[CompanyRepository]
        company       <- repo.create(rtjvm)
        fetchedById   <- repo.getById(company.id)
        fetchedBySlug <- repo.getBySlug(company.slug)
      } yield (company, fetchedById, fetchedBySlug)

      program.assert {
        case (company, fetchedById, fetchedBySlug) =>
          fetchedById.contains(company) && fetchedBySlug.contains(company)
        case _ => false
      }

    },
    test("update record") {
      val program = for {
        repo        <- ZIO.service[CompanyRepository]
        company     <- repo.create(rtjvm)
        updated     <- repo.update(company.id, (c => c.copy(url = "blog.rockthejvm.com")))
        fetchedById <- repo.getById(company.id)
      } yield (updated, fetchedById)

      program.assert {
        case (updated, fetchedById) =>
          fetchedById.contains(updated)
        case _ => false
      }
    },
    test("delete record") {
      val program = for {
        repo        <- ZIO.service[CompanyRepository]
        company     <- repo.create(rtjvm)
        _           <- repo.delete(company.id)
        fetchedById <- repo.getById(company.id)
      } yield fetchedById

      program.assert(_.isEmpty)
    },
    test("get all records") {
      val program = for {
        repo             <- ZIO.service[CompanyRepository]
        companies        <- ZIO.collectAll((1 to 10).map(_ => repo.create(genCompany())))
        companiesFetched <- repo.get
      } yield (companies, companiesFetched)

      program.assert {
        case (companies, companiesFetched) =>
          companies.toSet == companiesFetched.toSet
        case _ => false
      }
    }
  ).provide(
    CompanyRepositoryLive.layer,
    dataSourceLayer,
    Repository.quillLayer,
    Scope.default
  )

}

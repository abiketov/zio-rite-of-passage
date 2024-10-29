package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.{Company, CompanyFilter}
import zio.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import io.getquill.autoQuote

trait CompanyRepository {

  def create(company: Company): Task[Company]
  def update(id: Long, op: Company => Company): Task[Company]
  def delete(id: Long): Task[Company]
  def getById(id: Long): Task[Option[Company]]
  def getBySlug(slug: String): Task[Option[Company]]
  def get: Task[List[Company]]
  def search(filter: CompanyFilter): Task[List[Company]]
  def uniqueAttributes: Task[CompanyFilter]
}

class CompanyRepositoryLive(quill: Quill.Postgres[SnakeCase]) extends CompanyRepository {

  import quill.*

  inline given schema: SchemaMeta[Company]  = schemaMeta[Company]("companies")
  inline given insMeta: InsertMeta[Company] = insertMeta[Company](_.id)
  inline given updMeta: UpdateMeta[Company] = updateMeta[Company](_.id)

  override def create(company: Company): Task[Company] =
    run {
      query[Company]
        .insertValue(lift(company))
        .returning(c => c)
    }

  override def getById(id: Long): Task[Option[Company]] =
    run {
      query[Company].filter(_.id == lift(id))
    }.map(_.headOption)

  override def getBySlug(slug: String): Task[Option[Company]] =
    run {
      query[Company].filter(_.slug == lift(slug))
    }.map(_.headOption)

  override def get: Task[List[Company]] = run(query[Company])

  override def update(id: Long, op: Company => Company): Task[Company] =
    for {
      current <- getById(id).someOrFail(
        new RuntimeException(s"Could not update, ud $id is missing")
      )
      updated <- run {
        query[Company]
          .filter(_.id == lift(current.id))
          .updateValue(lift(op(current)))
          .returning(r => r)
      }
    } yield updated

  override def delete(id: Long): Task[Company] =
    run {
      query[Company]
        .filter(_.id == lift(id))
        .delete
        .returning(r => r)
    }

  override def uniqueAttributes: Task[CompanyFilter] = {
    for {
      locations <- run(query[Company].map(_.location).distinct).map(list =>
        list.flatMap(o => o.toList)
      )
      countries <- run(query[Company].map(_.country).distinct).map(list =>
        list.flatMap(o => o.toList)
      )
      industries <- run(query[Company].map(_.industry).distinct).map(list =>
        list.flatMap(o => o.toList)
      )
      tags <- run(query[Company].map(_.tags)).map(listOfList => listOfList.flatten.toSet.toList)
    } yield CompanyFilter(locations, countries, industries, tags)
  }

  /** select * from companies where location in filter.locations or country in ... or industry in
    * ... or tags in (select c1.tags from companies c1 where c1.id == company.id)
    *
    * @param filter
    * @return
    */
  override def search(filter: CompanyFilter): Task[List[Company]] = {
    if (filter.isEmpty) this.get
    else {
      run {
        query[Company]
          .filter { company =>
            liftQuery(filter.locations.toSet).contains(company.location) ||
            liftQuery(filter.countries.toSet).contains(company.country) ||
            liftQuery(filter.industries.toSet).contains(company.industry) ||
            sql"${company.tags} && ${lift(filter.tags)}".asCondition
          }
      }
    }

  }

}

object CompanyRepositoryLive {

  val layer = ZLayer {
    ZIO.service[Quill.Postgres[SnakeCase.type]].map(quill => CompanyRepositoryLive(quill))
  }
}

object CompanyRepositoryDemo extends ZIOAppDefault {

  val program = for {
    repo <- ZIO.service[CompanyRepository]
    c    <- repo.create(Company(-1L, "rock-the-jvm", "Rock the JVM", "rockthejvm.com"))
    _    <- ZIO.succeed(println(c))
    _    <- repo.create(Company(-1L, "abc-rocks", "ABC Rocks", "abcrocks.com"))
    _    <- repo.create(Company(-1L, "rock-jvm", "Rock JVM", "rockjvm.com"))
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =
    program.provide(
      CompanyRepositoryLive.layer,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("rockthejvm.db")
    )
}

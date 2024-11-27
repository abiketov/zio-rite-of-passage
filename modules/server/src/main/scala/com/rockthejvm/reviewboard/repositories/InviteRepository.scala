package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.domain.data.*
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait InviteRepository {

  // TODO
  def getInvitesByUserName(userName: String): Task[List[InviteNamedRecord]]
  def getInvitePack(userName: String, companyId: Long): Task[Option[InviteRecord]]
  def addInvitePack(userName: String, companyId: Long, nInvites: Int): Task[Long]
  def activatePack(id: Long): Task[Boolean]
  def markInvites(userName: String, companyId: Long, nInvites: Int): Task[Int]
}

class InviteRepositoryLive private (quill: Quill.Postgres[SnakeCase]) extends InviteRepository {

  import quill.*

  inline given companySchema: SchemaMeta[Company] = schemaMeta[Company]("companies")

  inline given companyInsMeta: InsertMeta[Company] = insertMeta[Company](_.id)

  inline given companyUpdMeta: UpdateMeta[Company] = updateMeta[Company](_.id)

  inline given schema: SchemaMeta[InviteRecord] = schemaMeta[InviteRecord]("invites")

  inline given insMeta: InsertMeta[InviteRecord] = insertMeta[InviteRecord](_.id)

  inline given updMeta: UpdateMeta[InviteRecord] = updateMeta[InviteRecord](_.id)

  /** select companyId, companyName, Invites from invites, companies where invites.userName = ? and
    * invites.active = true and invites.nInvites > 0 and invites.companyId=company.companyId
    *
    * @param userName
    * @return
    */
  override def getInvitesByUserName(userName: String): Task[List[InviteNamedRecord]] =
    run(
      for {
        record <- query[InviteRecord]
          .filter(_.userName == lift(userName))
          .filter(_.nInvites > 0)
          .filter(_.active == true)
        company <- query[Company] if (company.id == record.companyId) // join condition
      } yield InviteNamedRecord(company.id, company.name, record.nInvites)
    )

  override def getInvitePack(userName: String, companyId: Long): Task[Option[InviteRecord]] =
    run(
      query[InviteRecord]
        .filter(_.companyId == lift(companyId))
        .filter(_.userName == lift(userName))
        .filter(_.active == true)
    ).map(_.headOption)

  /** WARNING: Adding multiple packs for the same company may cause unexpected behavior
    *
    * @param userName
    * @param companyId
    * @param packSize
    * @return
    */
  override def addInvitePack(userName: String, companyId: Long, nInvites: Int): Task[Long] =
    run(
      query[InviteRecord]
        .insertValue(lift(InviteRecord(-1, userName, companyId, nInvites, false)))
        .returning(_.id)
    )

  override def activatePack(id: Long): Task[Boolean] = {
    for {
      current <- run(query[InviteRecord].filter(_.id == lift(id)))
        .map(_.headOption)
        .someOrFail(new RuntimeException(s"Unable to activate pack with id $id: no such pack"))
      result <- run(
        query[InviteRecord]
          .filter(_.id == lift(id))
          .updateValue(lift(current.copy(active = true)))
          .returning((_ => true))
      )
    } yield result
  }

  override def markInvites(userName: String, companyId: Long, nInvites: Int): Task[Int] =
    for {
      currentRecord <- getInvitePack(userName, companyId)
        .someOrFail(new RuntimeException(s"User $userName cannot send invites for company $companyId"))
      nInvitesMarked <- ZIO.succeed(Math.min(nInvites, currentRecord.nInvites))
      _ <- run(
        query[InviteRecord]
          .filter(_.id == lift(currentRecord.id))
          .updateValue(lift(currentRecord.copy(nInvites = currentRecord.nInvites - nInvitesMarked )))
          .returning(r => r)
        
      )
    } yield nInvitesMarked
}

object InviteRepositoryLive {

  val layer = ZLayer {
    for {
      quill <- ZIO.service[Quill.Postgres[SnakeCase.type]]
    } yield new InviteRepositoryLive(quill)
  }
}

object InviteRepositoryDemo extends ZIOAppDefault {

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = {
    val program = for {
      repo    <- ZIO.service[InviteRepository]
      records <- repo.getInvitesByUserName("daniel@rockthejvm.com")
      _       <- Console.printLine(s"Records:$records")
    } yield ()

    program.provide(
      InviteRepositoryLive.layer,
      Repository.dataLayer
    )
  }
}

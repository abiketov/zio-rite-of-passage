package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.*
import com.rockthejvm.reviewboard.config.*
import com.rockthejvm.reviewboard.repositories.{CompanyRepository, InviteRepository}
import zio.*

trait InviteService {

  def getInvitesByUserName(userName: String): Task[List[InviteNamedRecord]]
  def sendInvites(userName: String, companyId: Long, receivers: List[String]): Task[Int]
  def addInvitePack(userName: String, companyId: Long): Task[Long]
  def activatePack(packId: Long): Task[Boolean]
}

class InviteServiceLive private (
    inviteRepo: InviteRepository,
    companyRepo: CompanyRepository,
    emailService: EmailService,
    config: InvitePackConfig
) extends InviteService {

  override def getInvitesByUserName(userName: String): Task[List[InviteNamedRecord]] =
    inviteRepo.getInvitesByUserName(userName)

  /** Only one invite pack per user per company
    *
    * @param userName
    * @param companyId
    * @return
    */
  override def addInvitePack(userName: String, companyId: Long): Task[Long] = {

    for {
      company <- companyRepo
        .getById(companyId)
        .someOrFail(
          new RuntimeException(s"Cannot invite to review:company with id $companyId not found")
        )
      currentPack <- inviteRepo.getInvitePack(userName, companyId)
      newPackId <- currentPack match {
        case None => inviteRepo.addInvitePack(userName, companyId, 200) // configure pack size 200
        case Some(_) =>
          ZIO.fail(new RuntimeException("You already have active pack for this company."))
      }
      // TODO: remove after introduction of payment process
      // _ <- inviteRepo.activatePack(newPackId)

    } yield newPackId

  }

  override def sendInvites(
      userName: String,
      companyId: Long,
      receivers: List[String]
  ): Task[Int] = for {
    company <- companyRepo
      .getById(companyId)
      .someOrFail(
        new RuntimeException(s"Cannot send invites:company with id $companyId not found")
      )
    nInvitesMarked <- inviteRepo.markInvites(userName, companyId, receivers.size)
    _ <- ZIO.collectAllPar(
      receivers
        .take(nInvitesMarked)
        .map(receiver => emailService.sendReviewInvite(userName, receiver, company))
    )

  } yield nInvitesMarked

  override def activatePack(packId: Long): Task[Boolean] = {
    println(s"Action activate pack $packId")
    inviteRepo.activatePack(packId)
  }

}

object InviteServiceLive {

  val layer = ZLayer {
    for {
      inviteRepo   <- ZIO.service[InviteRepository]
      companyRepo  <- ZIO.service[CompanyRepository]
      emailService <- ZIO.service[EmailService]
      config       <- ZIO.service[InvitePackConfig]
    } yield new InviteServiceLive(inviteRepo, companyRepo, emailService, config)
  }

  val configuredLayer = Configs.makeLayer[InvitePackConfig]("rockthejvm.invites") >>> layer
}

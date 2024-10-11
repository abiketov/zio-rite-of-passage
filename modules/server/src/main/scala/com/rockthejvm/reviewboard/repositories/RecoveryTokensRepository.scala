package com.rockthejvm.reviewboard.repositories

import com.rockthejvm.reviewboard.config.{Configs, RecoveryTokensConfig}
import com.rockthejvm.reviewboard.domain.data.PasswordRecoveryToken
import io.getquill.*
import io.getquill.jdbczio.Quill
import zio.*

trait RecoveryTokensRepository {

  def getToken(email: String): Task[Option[String]]
  def checkToken(email: String, token: String): Task[Boolean]

}

class RecoveryTokensRepositoryLive private (
    tokenConfig: RecoveryTokensConfig,
    quill: Quill.Postgres[SnakeCase],
    userRepo: UserRepository
) extends RecoveryTokensRepository {

  import quill.*

  inline given schemaMetaRecoveryToken: SchemaMeta[PasswordRecoveryToken] =
    schemaMeta[PasswordRecoveryToken]("recovery_tokens")

  inline given insMeta: InsertMeta[PasswordRecoveryToken] = insertMeta[PasswordRecoveryToken]()

  inline given upMeta: UpdateMeta[PasswordRecoveryToken] =
    updateMeta[PasswordRecoveryToken](_.email)

  private val tokenDuration = 600

  override def getToken(email: String): Task[Option[String]] = {
    // ZIO.fail(new RuntimeException("Not implemented"))
    // check the user in the database
    userRepo.getByEmail(email).flatMap {
      case None    => ZIO.none
      case Some(_) => makeFreshToken(email).map(Some(_))
    }
  }

  private def randomUpperCaseString(len: Int): Task[String] =
    ZIO.succeed(scala.util.Random.alphanumeric.take(len).mkString.toUpperCase)

  private def findToken(email: String): Task[Option[String]] = {
    // select token from recovery_tokens where email = ?
    run(query[PasswordRecoveryToken].filter(_.email == lift(email))).map(_.headOption.map(_.token))
  }

  private def replaceToken(email: String): Task[String] =
    for {
      token <- randomUpperCaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .updateValue(
            lift(
              PasswordRecoveryToken(
                email,
                token,
                java.lang.System.currentTimeMillis() + tokenDuration * 1000
              )
            )
          )
          .returning(r => r)
      )
    } yield token

  private def generateToken(email: String): Task[String] =
    for {
      token <- randomUpperCaseString(8)
      _ <- run(
        query[PasswordRecoveryToken]
          .insertValue(
            lift(
              PasswordRecoveryToken(
                email,
                token,
                java.lang.System.currentTimeMillis() + tokenDuration * 1000
              )
            )
          )
          .returning(r => r)
      )
    } yield token

  private def makeFreshToken(email: String): Task[String] = {
    // find token in the table
    // if so replace if not create
    findToken(email).flatMap {
      case Some(token) => replaceToken(email)
      case None        => generateToken(email)
    }
  }

  override def checkToken(email: String, token: String): Task[Boolean] =
    run(query[PasswordRecoveryToken].filter(r => r.email == lift(email) && r.token == lift(token)))
      .map(_.nonEmpty)
}

object RecoveryTokensRepositoryLive {

  val layer = ZLayer {
    for {
      config   <- ZIO.service[RecoveryTokensConfig]
      quill    <- ZIO.service[Quill.Postgres[SnakeCase.type]]
      userRepo <- ZIO.service[UserRepository]
    } yield new RecoveryTokensRepositoryLive(config, quill, userRepo)
  }

  val configuredLayer =
    Configs.makeLayer[RecoveryTokensConfig]("rockthejvm.recoveryTokens") >>> layer
}

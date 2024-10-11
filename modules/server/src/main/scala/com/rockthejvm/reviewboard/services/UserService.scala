package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.{User, UserToken}
import com.rockthejvm.reviewboard.repositories.{RecoveryTokensRepository, UserRepository}
import zio.*

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

trait UserService {

  def registerUser(email: String, password: String): Task[User]
  def verifyPassword(email: String, password: String): Task[Boolean]
  def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User]
  def deleteUser(email: String, password: String): Task[User]

  def generateToken(email: String, password: String): Task[Option[UserToken]]

  // password recovery flow
  def sendPasswordRecoveryToken(email: String): Task[Unit]
  def recoverPasswordFromToken(email: String, token: String, newPassword: String): Task[Boolean]
}

class UserServiceLive private (
    jwtService: JWTService,
    emailService: EmailService,
    userRepo: UserRepository,
    tokenRepo: RecoveryTokensRepository
) extends UserService {

  override def registerUser(email: String, password: String): Task[User] =
    userRepo.create(
      User(
        id = -1L,
        email = email,
        hashedPassword = UserServiceLive.Hasher.generateHash(password)
      )
    )

  /** @param email
    * @param password
    * @return
    */
  override def verifyPassword(email: String, password: String): Task[Boolean] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
      result <- existingUser match {
        case Some(user) =>
          ZIO
            .attempt(UserServiceLive.Hasher.validateHash(password, user.hashedPassword))
            .orElseSucceed(false)
        case None => ZIO.succeed(false)
      }
    } yield result

  /** @param email
    * @param oldPassword
    * @param newPassword
    * @return
    */
  override def updatePassword(email: String, oldPassword: String, newPassword: String): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"User with email $email is not found"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(oldPassword, existingUser.hashedPassword)
      )
      updatedUser <- userRepo
        .update(
          existingUser.id,
          user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
        )
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not update password for email $email"))

    } yield updatedUser

  /** @param email
    * @param password
    * @return
    */
  override def deleteUser(email: String, password: String): Task[User] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"User with email $email is not found"))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      deletedUser <- userRepo
        .delete(existingUser.id)
        .when(verified)
        .someOrFail(new RuntimeException(s"Could not update password for email $email"))

    } yield deletedUser

  /** @param email
    * @param password
    * @return
    */
  override def generateToken(email: String, password: String): Task[Option[UserToken]] =
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"User with email $email is not found"))
      _ <- ZIO.succeed(println(existingUser))
      verified <- ZIO.attempt(
        UserServiceLive.Hasher.validateHash(password, existingUser.hashedPassword)
      )
      maybeToken <- jwtService.createToken(existingUser).when(verified)

    } yield {
      println(maybeToken)
      maybeToken
    }

  override def sendPasswordRecoveryToken(email: String): Task[Unit] = {
    // ZIO.fail(new RuntimeException("Not implemented yet!"))
    // get a token from the tokenRepo
    // send email with the token
    tokenRepo.getToken(email).flatMap {
      case Some(token) => emailService.sendPasswordRecoveryEmail(email, token)
      case None        => ZIO.unit
    }

  }

  override def recoverPasswordFromToken(
      email: String,
      token: String,
      newPassword: String
  ): Task[Boolean] = {
    // ZIO.fail(new RuntimeException("Not implemented yet!"))
    for {
      existingUser <- userRepo
        .getByEmail(email)
        .someOrFail(new RuntimeException(s"User with email ${email} not found!"))
      tokenIsValid <- tokenRepo.checkToken(email, token)
      result <- userRepo
        .update(
          existingUser.id,
          user => user.copy(hashedPassword = UserServiceLive.Hasher.generateHash(newPassword))
        )
        .when(tokenIsValid)
        .map(_.nonEmpty)
    } yield result
  }
}

object UserServiceLive {

  val layer = ZLayer {
    for {
      userRepo     <- ZIO.service[UserRepository]
      emailService <- ZIO.service[EmailService]
      jwtService   <- ZIO.service[JWTService]
      tokenRepo    <- ZIO.service[RecoveryTokensRepository]
    } yield new UserServiceLive(jwtService, emailService, userRepo, tokenRepo)
  }

  object Hasher {

    private val PBKDF2_ALGORITHM  = "PBKDF2WithHmacSHA512"
    private val PBKDF2_ITERATIONS = 1000
    private val SALT_BYTE_SIZE    = 24
    private val HASH_BYTE_SIZE    = 24

    private val skf: SecretKeyFactory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)

    private def pbkdf2(
        message: Array[Char],
        salt: Array[Byte],
        iterations: Int,
        nBytes: Int
    ): Array[Byte] = {
      val keySpec: PBEKeySpec = new PBEKeySpec(message, salt, iterations, nBytes * 8)
      skf.generateSecret(keySpec).getEncoded()
    }

    private def toHex(array: Array[Byte]): String = {
      array.map(b => "%02X".format(b)).mkString
    }

    private def fromHex(string: String): Array[Byte] =
      string.sliding(2, 2).toArray.map { hexValue =>
        Integer.parseInt(hexValue, 16).toByte
      }

    /** a(i) ^ b(i) for every i should be 0
      *
      * @param a
      * @param b
      * @return
      */
    private def compareBytes(a: Array[Byte], b: Array[Byte]): Boolean = {
      val range = 0 until math.min(a.length, b.length)
      val diff = range.foldLeft(a.length ^ b.length) { case (accum, i) =>
        accum | (a(i) ^ b(i))
      }
      diff == 0
    }

    // hex-encoded bytes

    // string + salt + nIteration PBKDF2
    // 1000:SALT:HASHED_PASSWORD
    def generateHash(str: String): String = {

      val rng: SecureRandom = new SecureRandom()
      val salt: Array[Byte] = Array.ofDim[Byte](SALT_BYTE_SIZE)
      rng.nextBytes(salt)
      val hashBytes = pbkdf2(str.toCharArray(), salt, PBKDF2_ITERATIONS, HASH_BYTE_SIZE)
      s"$PBKDF2_ITERATIONS:${toHex(salt)}:${toHex(hashBytes)}"

    }

    def validateHash(password: String, hash: String): Boolean = {

      val hashSegments      = hash.split(":")
      val nIterations       = hashSegments(0).toInt
      val salt: Array[Byte] = fromHex(hashSegments(1))
      val validHash         = fromHex(hashSegments(2))
      val testHash          = pbkdf2(password.toCharArray(), salt, nIterations, HASH_BYTE_SIZE)
      compareBytes(testHash, validHash)
    }
  }

}

object UserServiceDemo {

  def main(args: Array[String]): Unit = {

    println(UserServiceLive.Hasher.generateHash("rockthejvm"))
    println(
      UserServiceLive.Hasher.validateHash(
        "rockthejvm",
        "1000:C5714C45AC5794A4481F94BB2D6FF76C4605F0FFFC09A07D:823075578976B713EAC4F4BF35ADE8B462DA1B77B7A5550C"
      )
    )
  }
}

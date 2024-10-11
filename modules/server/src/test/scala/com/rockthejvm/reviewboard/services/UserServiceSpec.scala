package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.domain.data.{User, UserId, UserToken}
import com.rockthejvm.reviewboard.repositories.{RecoveryTokensRepository, UserRepository}
import zio.*
import zio.test.{assertTrue, Spec, TestEnvironment, ZIOSpecDefault}

object UserServiceSpec extends ZIOSpecDefault {

  val daniel = User(
    1L,
    "daniel@rockthejvm.com",
    "1000:C5714C45AC5794A4481F94BB2D6FF76C4605F0FFFC09A07D:823075578976B713EAC4F4BF35ADE8B462DA1B77B7A5550C"
  )

  val stubRepoLayer = ZLayer.succeed {
    new UserRepository {

      val db = collection.mutable.Map[Long, User](1L -> daniel)

      override def create(user: User): Task[User] = ZIO.succeed {
        db += (user.id -> user)
        user
      }

      override def update(id: Long, op: User => User): Task[User] = ZIO.attempt {
        val newUser = op(db(id))
        db += newUser.id -> newUser
        newUser

      }

      override def getById(id: Long): Task[Option[User]] = ZIO.succeed {
        db.get(id)
      }

      override def getByEmail(email: String): Task[Option[User]] = ZIO.succeed {
        db.values.find(_.email == email)
      }

      override def delete(id: Long): Task[User] = ZIO.attempt {
        val user = db(id)
        db -= id
        user
      }
    }
  }

  val stubTokenRepoLayer = ZLayer.succeed {
    new RecoveryTokensRepository {
      val db = collection.mutable.Map[String, String]()

      override def getToken(email: String): Task[Option[String]] =
        ZIO.attempt {
          val token = util.Random.alphanumeric.take(8).mkString.toUpperCase
          db += (email -> token)
          Some(token)
        }

      override def checkToken(email: String, token: String): Task[Boolean] =
        ZIO.succeed(db.get(email).filter(_ == token).nonEmpty)
    }
  }

  val stubEmailServiceLayer = ZLayer.succeed {
    new EmailService:
      override def sendEmail(to: String, subject: String, content: String): Task[Unit] = ZIO.unit

      override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = ZIO.unit
  }

  val stubJwtLayer = ZLayer.succeed {
    new JWTService {
      override def createToken(user: User): Task[UserToken] =
        ZIO.succeed(UserToken(user.email, "SECRET", 10000))

      override def verifyToken(token: String): Task[UserId] =
        ZIO.succeed(UserId(daniel.id, daniel.email))
    }
  }

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    suite("UserServiceSpec")(
      test("create and validate user") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.registerUser(daniel.email, "rockthejvm")
          valid   <- service.verifyPassword(daniel.email, "rockthejvm")
        } yield assertTrue(valid)
      },
      test("validate correct credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "rockthejvm")
        } yield assertTrue(valid)
      },
      test("invalidate incorrect credentials") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword(daniel.email, "rockthejvm1")
        } yield assertTrue(!valid)
      },
      test("invalidate non-existing user") {
        for {
          service <- ZIO.service[UserService]
          valid   <- service.verifyPassword("someemail@gmail.com", "rockthejvm")
        } yield assertTrue(!valid)
      },
      test("update password") {
        for {
          service     <- ZIO.service[UserService]
          updatedUser <- service.updatePassword(daniel.email, "rockthejvm", "rockthejvm1")
          oldValid    <- service.verifyPassword(daniel.email, "rockthejvm")
          newValid    <- service.verifyPassword(daniel.email, "rockthejvm1")
        } yield assertTrue(
          !oldValid && newValid
        )
      },
      test("delete non-existent user") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser("someemail@gmail.com", "rockthejvm").flip

        } yield assertTrue(
          err.isInstanceOf[RuntimeException]
        )
      },
      test("delete user with incorrect password") {
        for {
          service <- ZIO.service[UserService]
          err     <- service.deleteUser(daniel.email, "rockthejvm1").flip

        } yield assertTrue(
          err.isInstanceOf[RuntimeException]
        )
      },
      test("delete user with correct password") {
        for {
          service <- ZIO.service[UserService]
          user    <- service.deleteUser(daniel.email, "rockthejvm")

        } yield assertTrue(
          user == daniel
        )
      }
    ).provide(
      UserServiceLive.layer,
      stubEmailServiceLayer,
      stubTokenRepoLayer,
      stubJwtLayer,
      stubRepoLayer
    )
  }
}

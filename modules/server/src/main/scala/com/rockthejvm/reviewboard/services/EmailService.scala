package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.{Configs, EmailServiceConfig}
import zio.*

import java.util.Properties
import javax.mail.internet.MimeMessage
import javax.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}

trait EmailService {

  def sendEmail(to: String, subject: String, content: String): Task[Unit]
  def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit]

}

class EmailServiceLive private (config: EmailServiceConfig) extends EmailService {

  private val smtpHost = config.host
  private val smtpPort = config.port
  private val user     = config.user
  private val pass     = config.pass

  private val propsResource: Task[Properties] = ZIO.succeed {
    val prop = new Properties()
    prop.put("mail.smtp.auth", true)
    prop.put("mail.smtp.starttls.enable", "true")
    prop.put("mail.smtp.host", smtpHost)
    prop.put("mail.smtp.port", smtpPort)
    prop.put("mail.smtp.ssl.trust", smtpHost)
    prop
  }

  private def createSession(props: Properties): Task[Session] = ZIO.succeed {
    Session.getInstance(
      props,
      new Authenticator {
        override protected def getPasswordAuthentication: PasswordAuthentication =
          new PasswordAuthentication(user, pass)
      }
    )
  }

  private def createMessage(from: String, to: String, subject: String, content: String)(
      session: Session
  ): Task[MimeMessage] = ZIO.succeed {
    val message = new MimeMessage(session);
    message.setFrom(from)
    message.setRecipients(Message.RecipientType.TO, to)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    message

  }

  override def sendEmail(to: String, subject: String, content: String): Task[Unit] = {
    val messageZIO = for {
      props   <- propsResource
      session <- createSession(props)
      message <- createMessage("daniel@rockthejvm.com", to, subject, content)(session)
    } yield message

    messageZIO.map(message => Transport.send(message))
  }

  override def sendPasswordRecoveryEmail(to: String, token: String): Task[Unit] = {
    val subject = "User password recovery"
    val contentStr: String =
      s"""
         |<div style="border: 1px solid black; padding: 20px; font-family: sans-serif;font-size: 20px;">
         | <h1> User password recovery </h1>
         | <p>
         |  Your password recovery token is:$token
         | </p>
         | <p> 
         | Token is valid for 10 minutes.
         | </p>
         |</div>
         |""".stripMargin

    sendEmail(to, subject, contentStr)
  }

}

object EmailServiceLive {

  val layer = ZLayer {
    ZIO.service[EmailServiceConfig].map(config => new EmailServiceLive(config))
  }

  val configuredLayer = Configs.makeLayer[EmailServiceConfig]("rockthejvm.email") >>> layer
}

object EmailServiceDemo extends ZIOAppDefault {

  val program = for {
    emailService <- ZIO.service[EmailService]
    _            <- emailService.sendPasswordRecoveryEmail("abc@abc.com", "AHGS768SJ")
    _            <- Console.printLine("Sent email.")
  } yield ()

  override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = program
    .provide(EmailServiceLive.configuredLayer)
}

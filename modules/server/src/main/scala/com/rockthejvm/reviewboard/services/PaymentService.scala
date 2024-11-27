package com.rockthejvm.reviewboard.services

import com.rockthejvm.reviewboard.config.{Configs, StripeConfig}
import com.stripe.model.checkout.Session
import com.stripe.param.checkout.SessionCreateParams
import com.stripe.Stripe as TheStripe
import com.stripe.net.Webhook
import zio.*

import scala.jdk.OptionConverters.*
import scala.util.{Failure, Success, Try}

trait PaymentService {

  // create checkout session
  def createCheckoutSession(invitePackId: Long, userName: String): Task[Option[Session]]

  // handle a webhook event
  def handleWebhookEvent[A](
      signature: String,
      payload: String,
      action: String => Task[A]
  ): Task[Option[A]]
}

class PaymentServiceLive private (config: StripeConfig) extends PaymentService {

  override def createCheckoutSession(invitePackId: Long, userName: String): Task[Option[Session]] =
    ZIO
      .attempt {
        SessionCreateParams
          .builder()
          .setMode(SessionCreateParams.Mode.PAYMENT)
          .setSuccessUrl(config.successUrl)
          .setCancelUrl(config.cancelUrl)
          .setCustomerEmail(userName)
          // my own payload
          .setClientReferenceId(invitePackId.toString)
          .setInvoiceCreation(
            SessionCreateParams.InvoiceCreation
              .builder()
              .setEnabled(true)
              .build()
          )
          .setPaymentIntentData(
            SessionCreateParams.PaymentIntentData
              .builder()
              .setReceiptEmail(userName)
              .build()
          )
          // need add product
          .addLineItem(
            SessionCreateParams.LineItem
              .builder()
              .setPrice(config.price) // unique id of your Stripe product
              .setQuantity(1L)
              .build()
          )
          .build()
      }
      .map(params => Session.create(params))
      .map(s => Option(s))
      .logError("Stripe session creation failed")
      .catchSome { case _ =>
        ZIO.none
      }

  override def handleWebhookEvent[A](
      signature: String,
      payload: String,
      action: String => Task[A]
  ): Task[Option[A]] =
    ZIO
      .attempt {
        // build webhook event
        Webhook.constructEvent(payload, signature, config.secret)
      }
      .flatMap { event =>
        // check event type
        event.getType() match {
          case "checkout.session.completed" =>
            // parse the event
            println(s"checkout.session.completed $event")
            ZIO.foreach(
              event
                .getDataObjectDeserializer()
                .getObject()
                .toScala
                .map(_.asInstanceOf[Session])
                .map { obj =>
                  val refId = obj.getClientReferenceId()
                  println(s"Reference client id:$refId")
                  refId
                }
            )(action)

          case _ => ZIO.none
        }
      }

}

object PaymentServiceLive {

  val layer = ZLayer {
    for {
      config <- ZIO.service[StripeConfig]
      _      <- ZIO.attempt(TheStripe.apiKey = config.key)
    } yield new PaymentServiceLive(config)
  }

  val configuredLayer = Configs.makeLayer[StripeConfig]("rockthejvm.stripe") >>> layer
}

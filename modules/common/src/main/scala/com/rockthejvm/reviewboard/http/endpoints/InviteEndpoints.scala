package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.data.*
import sttp.tapir.*
import sttp.tapir.json.zio.*
import sttp.tapir.generic.auto.*
import com.rockthejvm.reviewboard.http.requests.*
import com.rockthejvm.reviewboard.http.responses.InviteResponse

trait InviteEndpoints extends BaseEndPoint {

  /** POST /invite/add input {companyId} 200 emails to invite people to write reviews output packId
    * as string
    */
  val addPackEndpoint = secureBaseEndpoint
    .tag("Invites")
    .name("Add invites")
    .description("Get invite tokens")
    .in("invite" / "add")
    .post
    .in(jsonBody[InvitePackRequest])
    .out(stringBody)

  /** POST /invite input JSON {} list of emails, companyId
    *
    * output { } number of invites that were sent, status this will send emails to users
    */

  val inviteEndpoint = secureBaseEndpoint
    .tag("Invites")
    .name("Invite")
    .description("Send people emails inviting them to leave review")
    .in("invite")
    .post
    .in(jsonBody[InviteRequest])
    .out(jsonBody[InviteResponse])

  /** GET /invite/all get all invites user sent
    */

  val getInvitesByUserIdEndpoint = secureBaseEndpoint
    .tag("Invites")
    .name("Get invites bby user Id")
    .description("Get all active invite packs fora user")
    .in("invite" / "all")
    .get
    .out(jsonBody[List[InviteNamedRecord]])

  // TODO - add paid endpoints

  val addPackPromotedEndpoint = secureBaseEndpoint
    .tag("Invites")
    .name("Add invites promoted")
    .description("Get invite tokens paid via stripe")
    .in("invite" / "promoted")
    .post
    .in(jsonBody[InvitePackRequest])
    .out(stringBody) // this is the Stripe checkout URL

  // webhook - will be called automatically by the service (Stripe)
  /** hit /invite/promoted -> Stripe checkout url go to URL, fill in the details , Pay Stripe will
    * call webhook -> activate the pack
    */

  val webhookEndpoint = baseEndPoint
    .tag("Invites")
    .name("Invite webhook")
    .description("Confirm the purchase of  an invite pack")
    .in("invite" / "webhook")
    .post
    .in(header[String]("Stripe-Signature"))
    .in(stringBody)

}

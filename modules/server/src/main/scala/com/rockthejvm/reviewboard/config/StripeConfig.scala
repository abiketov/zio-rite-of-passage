package com.rockthejvm.reviewboard.config

case class StripeConfig(
    key: String,
    secret: String,
    price: String,
    successUrl: String,
    cancelUrl: String
)

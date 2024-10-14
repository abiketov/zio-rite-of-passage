package com.rockthejvm.reviewboard.http.endpoints

import com.rockthejvm.reviewboard.domain.errors.HttpError
import sttp.tapir.*

trait BaseEndPoint {

  val baseEndPoint = endpoint
    .errorOut(statusCode and plainBody[String]) // Status Code , String
    .mapErrorOut[Throwable](
      /*(StatusCode, String) => MyHttpError */
      HttpError.decode
    )(
      /* MyHttpError => (StatusCode, String) */
      HttpError.encode
    )

  val secureBaseEndpoint = baseEndPoint.securityIn(auth.bearer[String]())
}

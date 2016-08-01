package org.http4s
package circe

import io.circe.jawn.CirceSupportParser.facade
import io.circe.{Decoder, Encoder, Json, Printer}
import org.http4s.headers.`Content-Type`

// Originally based on ArgonautInstances
trait CirceInstances {
  implicit val jsonDecoder: EntityDecoder[Json] = jawn.jawnDecoder(facade)

  def jsonOf[A](implicit decoder: Decoder[A]): EntityDecoder[A] =
    jsonDecoder.flatMapR { json =>
      decoder.decodeJson(json).fold(
        failure =>
          DecodeResult.failure(InvalidMessageBodyFailure(s"Could not decode JSON: $json", Some(failure))),
        DecodeResult.success(_)
      )
    }

  implicit val jsonEncoder: EntityEncoder[Json] =
    EntityEncoder[String].contramap[Json] { json =>
      // Comment from ArgonautInstances (which this code is based on):
      // TODO naive implementation materializes to a String.
      // See https://github.com/non/jawn/issues/6#issuecomment-65018736
      Printer.noSpaces.pretty(json)
    }.withContentType(`Content-Type`(MediaType.`application/json`))

  def jsonEncoderOf[A](implicit encoder: Encoder[A]): EntityEncoder[A] =
    jsonEncoder.contramap[A](encoder.apply)
}

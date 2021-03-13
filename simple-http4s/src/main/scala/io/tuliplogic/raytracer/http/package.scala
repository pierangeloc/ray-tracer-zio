package io.tuliplogic.raytracer

import eu.timepit.refined.W
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import io.circe.{Decoder, Encoder, KeyDecoder, KeyEncoder}
import sttp.tapir.Schema
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import zio.Has
package object http {
  // ----- Coercible codecs -----
  implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
    Decoder[B].map(_.coerce[A])

  implicit def coercibleEncoder[A: Coercible[*, B], B: Encoder]: Encoder[A] =
    Encoder[B].contramap(_.coerce[B])

  implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]: KeyDecoder[A] =
    KeyDecoder[B].map(_.coerce[A])

  implicit def coercibleKeyEncoder[A: Coercible[*, B], B: KeyEncoder]: KeyEncoder[A] =
    KeyEncoder[B].contramap[A](_.coerce[B])

  implicit def coercibleSchema[
    A: Coercible[B, *],
    B: Schema
  ]: Schema[A] = {
    val bSchema = implicitly[Schema[B]]
    Schema[A](bSchema.schemaType, bSchema.isOptional, bSchema.description, bSchema.format)
  }

  type EmailPred = MatchesRegex[W.`"""^[A-Za-z0-9+_.-]+@(.+)$"""`.T]
  type EmailValue = String Refined EmailPred

  type Configuration = Has[Config]
}

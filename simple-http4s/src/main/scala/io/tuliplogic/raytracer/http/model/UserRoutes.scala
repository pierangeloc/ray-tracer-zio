package io.tuliplogic.raytracer.http.model

import eu.timepit.refined._
import eu.timepit.refined.auto._
import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.MatchesRegex
import eu.timepit.refined.types.string.NonEmptyString
import io.circe._
import io.circe.generic.auto._
import io.estatico.newtype.macros._
import io.circe.refined._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import sttp.tapir._
import sttp.tapir.json.circe._
import io.tuliplogic.raytracer.http.model.UserRoutes.model.{CreateUser, Error, UserCreated}

object UserRoutes {
  val createUser: Endpoint[CreateUser, Error, UserCreated, Nothing] =
    endpoint.post.in("user").in(jsonBody[CreateUser]).out(jsonBody[UserCreated]).errorOut(jsonBody[Error])

  object model {
    // ----- Coercible codecs -----
    implicit def coercibleDecoder[A: Coercible[B, *], B: Decoder]: Decoder[A] =
      Decoder[B].map(_.coerce[A])

    implicit def coercibleEncoder[A: Coercible[B, *], B: Encoder]: Encoder[A] =
      Encoder[B].contramap(_.repr.asInstanceOf[B])

    implicit def coercibleKeyDecoder[A: Coercible[B, *], B: KeyDecoder]: KeyDecoder[A] =
      KeyDecoder[B].map(_.coerce[A])

    implicit def coercibleKeyEncoder[A: Coercible[B, *], B: KeyEncoder]: KeyEncoder[A] =
      KeyEncoder[B].contramap[A](_.repr.asInstanceOf[B])



    //    type EmailPred = MatchesRegex[W.`"""(?=[^\\s]+)(?=(\\w+)@([\\w\\.]+))"""`.T]
    type EmailPred = MatchesRegex[W.`"""^[A-Za-z0-9+_.-]+@(.+)$"""`.T]
    type EmailValue = String Refined EmailPred

    @newtype case class AccessToken(value: NonEmptyString)
    @newtype case class UserId(value: NonEmptyString)
    @newtype case class Email(value: EmailValue)

    case class CreateUser(email: Email)
    case class UserCreated(userId: UserId, accessToken: AccessToken)
    case class Error(code: Int, message: String)


    import io.circe.syntax._
    val cu = CreateUser(Email("pierangeloc@gmail.com"))
    cu.asJson
  }
}

package io.tuliplogic.raytracer.http.model.attapirato

import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype

object types {

  case class AppError(code: Int, message: String)

  object user {
    @newtype case class AccessToken(value: NonEmptyString)
    @newtype case class UserId(value: NonEmptyString)
    @newtype case class Email(value: EmailValue)

    case class CreateUser(email: Email)
    case class UserCreated(userId: UserId, accessToken: AccessToken)
  }




}
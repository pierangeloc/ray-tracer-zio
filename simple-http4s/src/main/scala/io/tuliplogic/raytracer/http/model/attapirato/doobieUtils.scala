package io.tuliplogic.raytracer.http.model.attapirato


import cats.Eq
import doobie.{Put, Read}
import io.estatico.newtype.Coercible

object doobieUtils {

  import io.estatico.newtype.ops._

  /**
    * Derive a Put (so it can be interpolated in doobie sql expressions) for any newtype backed by a type that is
    * supported by doobie (e.g. any wrapper of String, UUID, Boolean etc)
    *
    * if a: A has a Coercible[A, B] means one can transform a to b: B via a.coerce[B]
    */
  implicit def newTypePut[N: Coercible[R, *], R: Put]: Put[N] = Put[R].contramap[N](_.repr.asInstanceOf[R])

  implicit def newTypeRead[N: Coercible[R, *], R: Read]: Read[N] = Read[R].map(_.asInstanceOf[N])

  /** If we have an Eq instance for Repr type R, derive an Eq instance for  NewType N. */
  implicit def coercibleEq[R, N](implicit ev: Coercible[Eq[R], Eq[N]], R: Eq[R]): Eq[N] =
    ev(R)

//
//  /*
//   * With this one can create value classes like these and embed them in doobie fragments
//   */
//  @newtype case class UserId(value: UUID)
//
//  val userId: UserId = ???
//  val fragment = sql"select * from users where user_id = $userId"
}

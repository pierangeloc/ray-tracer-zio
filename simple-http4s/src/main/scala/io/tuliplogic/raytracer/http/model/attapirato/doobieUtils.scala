package io.tuliplogic.raytracer.http.model.attapirato


import cats.data.NonEmptyList
import cats.{Eq, Show}
import doobie.{Get, Put, Read}
import io.circe.Json
import io.estatico.newtype.Coercible
import org.postgresql.util.PGobject

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

  implicit val showPGobject: Show[PGobject] = Show.show(_.getValue.take(250))

  implicit val jsonGet: Get[Json] = {
    import cats.implicits._
    import io.circe.parser._
    Get.Advanced.other[PGobject](NonEmptyList.of("json")).temap[Json] { o =>
      parse(o.getValue).leftMap(_.show)
    }
  }

  implicit val jsonPut: Put[Json] =
    Put.Advanced.other[PGobject](NonEmptyList.of("json")).tcontramap[Json] { j =>
      val o = new PGobject
      o.setType("json")
      o.setValue(j.noSpaces)
      o
    }
}

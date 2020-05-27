package io.tuliplogic.raytracer.http.model.attapirato

object endpoints {
  import types.{Error, CreateUser, UserCreated}
  import sttp.tapir._
  import sttp.tapir.json.circe._
  import io.circe.generic.auto._
  import io.circe._
  import io.circe.refined._

  val createUser: Endpoint[CreateUser, Error, UserCreated, Nothing] =
    endpoint.post.in("user").in(jsonBody[CreateUser]).out(jsonBody[UserCreated]).errorOut(jsonBody[Error])

}

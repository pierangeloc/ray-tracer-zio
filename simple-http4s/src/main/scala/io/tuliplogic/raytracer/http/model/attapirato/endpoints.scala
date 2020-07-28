package io.tuliplogic.raytracer.http.model.attapirato

import io.tuliplogic.raytracer.http.model.attapirato.types.drawing.{DrawResponse, Scene}
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.{AccessToken, CreateUser, UserCreated, UserId}
import zio.{App, ExitCode, Task, UIO, ZIO}

object endpoints {
  import sttp.tapir._
  import sttp.tapir.json.circe._
  import io.circe.generic.auto._
  import io.circe.refined._

  val createUser: Endpoint[CreateUser, AppError, UserCreated, Nothing] =
    endpoint.post.in("user").in(jsonBody[CreateUser]).out(jsonBody[UserCreated]).errorOut(jsonBody[AppError])
    .description("Create a user")

  val drawImage: Endpoint[Scene, AppError, DrawResponse, Nothing] =
    endpoint.post.in("scene").in(jsonBody[Scene]).out(jsonBody[DrawResponse]).errorOut(jsonBody[AppError])
    .description("Draw an image from a given Scene description")
}

object zioEndpoints {
  import sttp.tapir.ztapir._
  import eu.timepit.refined.auto._

  val t: ZServerEndpoint[Any, CreateUser, AppError, UserCreated] =
    endpoints.createUser.zServerLogic(_ => UIO(UserCreated(UserId("123"), AccessToken("456"))))
}


object SimpleApp extends App {


  import zio.interop.catz._
  import zio.interop.catz.implicits._
  import sttp.tapir.server.http4s.ztapir._
  import org.http4s.syntax.kleisli._

  import org.http4s.server.Router
  import org.http4s.server.blaze.BlazeServerBuilder

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    (serve orElse zio.console.putStrLn("ERROR")).as(ExitCode.success)

  val userRoutes = zioEndpoints.t.toRoutes

  val serve: Task[Unit] = ZIO
    .runtime[Any]
    .flatMap { implicit rts =>
      BlazeServerBuilder[Task](rts.platform.executor.asEC)
        .bindHttp(8090, "localhost")
        .withHttpApp(Router("/"-> userRoutes ).orNotFound)
        .serve
        .compile
        .drain
    }
}
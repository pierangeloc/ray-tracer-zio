package io.tuliplogic.raytracer.http.model.attapirato

import io.tuliplogic.raytracer.http.model.attapirato.types.drawing.{DrawResponse, DrawingId, DrawingStatus, Scene}
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.APIError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.{AccessToken, CreateUser, UserCreated, UserId}
import org.http4s.HttpRoutes
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.{App, ExitCode, Task, UIO, ZIO}

object endpoints {
  import sttp.tapir._
  import sttp.tapir.json.circe._
  import io.circe.generic.auto._
  import io.circe.refined._

  val createUser: Endpoint[CreateUser, APIError, UserCreated, Nothing] =
    endpoint.post.in("user").in(jsonBody[CreateUser]).out(jsonBody[UserCreated]).errorOut(jsonBody[APIError])
    .description("Create a user")

  val drawImage: Endpoint[Scene, APIError, DrawResponse, Nothing] =
    endpoint.post.in("scene").in(jsonBody[Scene]).out(jsonBody[DrawResponse]).errorOut(jsonBody[APIError])
    .description("Draw an image from a given Scene description")
}

object zioEndpoints {
  import sttp.tapir.ztapir._
  import eu.timepit.refined.auto._

  val t: ZServerEndpoint[Any, CreateUser, APIError, UserCreated] =
    endpoints.createUser.zServerLogic(_ => UIO(UserCreated(UserId("123"), AccessToken("456"))))

  val draw: ZServerEndpoint[Any, Scene, APIError, DrawResponse] =
    endpoints.drawImage.zServerLogic(_ => UIO(DrawResponse(DrawingId("123"), DrawingStatus.Done)))
}


object SimpleApp extends App {


  import zio.interop.catz._
  import zio.interop.catz.implicits._
  import sttp.tapir.server.http4s.ztapir._
  import org.http4s.syntax.kleisli._

  import org.http4s.server.Router
  import org.http4s.server.blaze.BlazeServerBuilder
  import sttp.tapir.docs.openapi._
  import sttp.tapir.openapi.Server
  import sttp.tapir.openapi.circe.yaml._
  import cats.implicits._


  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    (serve orElse zio.console.putStrLn("ERROR")).as(ExitCode.success)

  val userRoutes: HttpRoutes[Task] = zioEndpoints.t.toRoutes
  val drawRoutes: HttpRoutes[Task] = zioEndpoints.draw.toRoutes
  val openApiDocs = endpoints.createUser.toOpenAPI("simple user management", "1.0")
    .servers(List(Server("localhost:8090").description("local server")))
  val docsRoutes = new SwaggerHttp4s(openApiDocs.toYaml)

  val allRoutes: HttpRoutes[Task] = docsRoutes.routes[Task] <+> userRoutes <+> drawRoutes

  val serve: Task[Unit] = ZIO
    .runtime[Any]
    .flatMap { implicit rts =>
      BlazeServerBuilder[Task](rts.platform.executor.asEC)
        .bindHttp(8090, "localhost")
        .withHttpApp(Router("/"-> allRoutes ).orNotFound)
        .serve
        .compile
        .drain
    }
}

//        .withHttpApp(Router("/"->( userRoutes <+> docsRoutes.routes).orNotFound))

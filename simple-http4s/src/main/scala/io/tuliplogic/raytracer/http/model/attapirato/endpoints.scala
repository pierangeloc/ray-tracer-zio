package io.tuliplogic.raytracer.http.model.attapirato

import java.util.UUID

import io.tuliplogic.raytracer.http.model.attapirato.types.drawing.{DrawResponse, DrawingId, DrawingStatus, Scene}
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.{APIError, BootstrapError}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.Event.{LoginSuccess, PasswordUpdated, UserCreated}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.Cmd.{CreateUser, Login, UpdatePassword}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.{User, UserId}
import io.tuliplogic.raytracer.http.model.attapirato.users.UsersRepo
import org.http4s.HttpRoutes
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.blocking.Blocking
import zio.{App, ExitCode, Task, UIO, URIO, ZIO, ZLayer}

object endpoints {
  import sttp.tapir._
  import sttp.tapir.json.circe._
  import io.circe.generic.auto._
  import io.circe.refined._

  val createUser: Endpoint[CreateUser, APIError, UserCreated, Nothing] =
    endpoint.post.in("user").in(jsonBody[CreateUser]).out(jsonBody[UserCreated]).errorOut(jsonBody[APIError])
      .description("Create a user")

  val updatePassword: Endpoint[UpdatePassword, APIError, PasswordUpdated, Nothing] =
    endpoint.post.in("user").in(jsonBody[UpdatePassword]).out(jsonBody[PasswordUpdated]).errorOut(jsonBody[APIError])
      .description("Update user password")


  val login: Endpoint[Login, APIError, LoginSuccess, Nothing] =
    endpoint.post.in("user").in(jsonBody[Login]).out(jsonBody[LoginSuccess]).errorOut(jsonBody[APIError])
      .description("Login to obtain an access token")

  val drawImage: Endpoint[Scene, APIError, DrawResponse, Nothing] =
    endpoint.post.in("scene").in(jsonBody[Scene]).out(jsonBody[DrawResponse]).errorOut(jsonBody[APIError])
      .description("Draw an image from a given Scene description")
}

object zioEndpoints {

  import sttp.tapir.ztapir._
  import eu.timepit.refined.auto._

  object user {
    val createUser: ZServerEndpoint[UsersRepo, CreateUser, APIError, UserCreated] =
      endpoints.createUser.zServerLogic( createUser =>
        UIO.effectTotal(UUID.randomUUID()).flatMap { id => {
          val userId = UserId(id)
          UsersRepo.createUser(User(userId, createUser.email, None, None))
            .bimap(_ => APIError(200, "Error creating user"), _ => UserCreated(userId))

        }
      }
    )



  }

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

  val layer: ZLayer[Blocking, types.AppError, UsersRepo] =
    (Config.fromTypesafeConfig() ++ ZLayer.identity[Blocking]) >>> DB.transactor >>> UsersRepo.doobieLive

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    (serve orElse zio.console.putStrLn("couldn't start the http server"))
      .provideCustomLayer(layer)
      .catchAll {
        case BootstrapError(_, _, _) => zio.console.putStrLn("Error bootstrapping")
        case other => zio.console.putStrLn(s"Other error at startup, $other")
      }.exitCode

  val userRoutes: URIO[UsersRepo, HttpRoutes[Task]] = zioEndpoints.user.createUser.toRoutesR
  val drawRoutes: URIO[Any, HttpRoutes[Task]] = zioEndpoints.draw.toRoutesR

  val openApiDocs: OpenAPI = endpoints.createUser.toOpenAPI("simple user management", "1.0")
    .servers(List(Server("localhost:8090").description("local server")))

  val docsRoutes: HttpRoutes[Task] = new SwaggerHttp4s(openApiDocs.toYaml).routes[Task]

  val serve = for {
    allRoutes <- ZIO.mapParN(userRoutes, drawRoutes)((r1, r2) => r1 <+> r2 <+> docsRoutes)
    _         <- serveRoutes(allRoutes)
  } yield ()

  def serveRoutes(rs: HttpRoutes[Task]): Task[Unit] = ZIO
    .runtime[Any]
    .flatMap { implicit rts =>
      BlazeServerBuilder[Task](rts.platform.executor.asEC)
        .bindHttp(8090, "localhost")
        .withHttpApp(Router("/"-> rs ).orNotFound)
        .serve
        .compile
        .drain
    }
}

//        .withHttpApp(Router("/"->( userRoutes <+> docsRoutes.routes).orNotFound))

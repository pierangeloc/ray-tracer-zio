package io.tuliplogic.raytracer.http.model.attapirato


import io.tuliplogic.raytracer.http.model.attapirato.types.drawing.{DrawResponse, SceneId, SceneStatus, SceneDescription}
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.APIError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.Event.{LoginSuccess, PasswordUpdated, UserCreated}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.Cmd.{CreateUser, Login, UpdatePassword}
import io.tuliplogic.raytracer.http.model.attapirato.users.Users
import org.http4s.HttpRoutes
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.logging.Logging
import zio.{Task, UIO, URIO, ZIO}

object endpoints {
  import sttp.tapir._
  import sttp.tapir.json.circe._
  import io.circe.generic.auto._
  import io.circe.refined._

  val createUser: Endpoint[CreateUser, APIError, UserCreated, Nothing] =
    endpoint.post.in("user").in(jsonBody[CreateUser]).out(jsonBody[UserCreated]).errorOut(jsonBody[APIError])
      .description("Create a user")

  val updatePassword: Endpoint[UpdatePassword, APIError, PasswordUpdated, Nothing] =
    endpoint.put.in("user").in(jsonBody[UpdatePassword]).out(jsonBody[PasswordUpdated]).errorOut(jsonBody[APIError])
      .description("Update user password")


  val login: Endpoint[Login, APIError, LoginSuccess, Nothing] =
    endpoint.post.in("login").in(jsonBody[Login]).out(jsonBody[LoginSuccess]).errorOut(jsonBody[APIError])
      .description("Login to obtain an access token")

  val drawImage: Endpoint[SceneDescription, APIError, DrawResponse, Nothing] =
    endpoint.post.in("scene").in(jsonBody[SceneDescription]).out(jsonBody[DrawResponse]).errorOut(jsonBody[APIError])
      .description("Draw an image from a given Scene description")
}

object zioEndpoints {

  import sttp.tapir.ztapir._
  import eu.timepit.refined.auto._

  object user {

    val createUser: ZServerEndpoint[Users, CreateUser, APIError, UserCreated] =
      endpoints.createUser.zServerLogic( createUser =>
        Users.createUser(createUser.email)
      )

    val updatePassword: ZServerEndpoint[Users, UpdatePassword, APIError, PasswordUpdated] =
      endpoints.updatePassword.zServerLogic( updatePwd =>
        Users.updatePassword(updatePwd.email, updatePwd.password)
      )

    val login: ZServerEndpoint[Users, Login, APIError, LoginSuccess] =
      endpoints.login.zServerLogic(login =>
        Users.login(login.email, login.password)
      )
  }

  val draw: ZServerEndpoint[Any, SceneDescription, APIError, DrawResponse] =
    endpoints.drawImage.zServerLogic(_ => UIO(DrawResponse(SceneId("123"), SceneStatus.Done)))
}


//object SimpleApp extends App {
//
//  import zio.interop.catz._
//  import zio.interop.catz.implicits._
//  import sttp.tapir.server.http4s.ztapir._
//  import org.http4s.syntax.kleisli._
//
//  import org.http4s.server.Router
//  import org.http4s.server.blaze.BlazeServerBuilder
//  import sttp.tapir.docs.openapi._
//  import sttp.tapir.openapi.Server
//  import sttp.tapir.openapi.circe.yaml._
//  import cats.implicits._
//
//  val slf4jLogger: ULayer[Logging] = Slf4jLogger.make((_, s) => s)
//
//  val layer: ZLayer[Blocking, types.AppError, UsersRepo] =
//    (((Config.fromTypesafeConfig() ++ ZLayer.identity[Blocking]) >>> DB.transactor) ++ slf4jLogger) >>> UsersRepo.doobieLive
//
//  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
//    serve
//      .catchAll {
//        case BootstrapError(_, _) => log.error("Error bootstrapping")
//        case other => log.throwable(s"Other error at startup", other)
//      }
//      .provideSomeLayer[zio.ZEnv](layer ++ slf4jLogger)
//      .exitCode
//
//
//  val userRoutes: URIO[Users with Logging, HttpRoutes[Task]] = zioEndpoints.user.createUser.toRoutesR
//  val drawRoutes: URIO[Any, HttpRoutes[Task]] = zioEndpoints.draw.toRoutesR
//
//  val openApiDocs: OpenAPI = endpoints.createUser.toOpenAPI("simple user management", "1.0")
//    .servers(List(Server("localhost:8090").description("local server")))
//
//  val docsRoutes: HttpRoutes[Task] = new SwaggerHttp4s(openApiDocs.toYaml).routes[Task]
//
//  val serve = for {
//    allRoutes <- ZIO.mapParN(userRoutes, drawRoutes)((r1, r2) => r1 <+> r2 <+> docsRoutes)
//    _         <- serveRoutes(allRoutes)
//  } yield ()
//
//  def serveRoutes(rs: HttpRoutes[Task]): Task[Unit] = ZIO
//    .runtime[Any]
//    .flatMap { implicit rts =>
//      BlazeServerBuilder[Task](rts.platform.executor.asEC)
//        .bindHttp(8090, "localhost")
//        .withHttpApp(Router("/"-> rs ).orNotFound)
//        .serve
//        .compile
//        .drain
//    }
//}

//        .withHttpApp(Router("/"->( userRoutes <+> docsRoutes.routes).orNotFound))


object AllRoutes {

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

    val createUser: URIO[Users with Logging, HttpRoutes[Task]] = zioEndpoints.user.createUser.toRoutesR
    val updateUserPwd: URIO[Users with Logging, HttpRoutes[Task]] = zioEndpoints.user.updatePassword.toRoutesR
    val loginUser: URIO[Users with Logging, HttpRoutes[Task]] = zioEndpoints.user.login.toRoutesR
    val drawRoutes: UIO[HttpRoutes[Task]] = zioEndpoints.draw.toRoutesR

    val openApiDocs: OpenAPI = endpoints.createUser.toOpenAPI("simple user management", "1.0")
      .servers(List(Server("localhost:8090").description("local server")))

    val docsRoutes: HttpRoutes[Task] = new SwaggerHttp4s(openApiDocs.toYaml).routes[Task]

    val allRoutes: List[URIO[Users with Logging, HttpRoutes[Task]]] = List(createUser, updateUserPwd, loginUser, drawRoutes)

  val serve = for {
      allRoutes <- ZIO.mergeAll(allRoutes)(docsRoutes)(_ <+> _)
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
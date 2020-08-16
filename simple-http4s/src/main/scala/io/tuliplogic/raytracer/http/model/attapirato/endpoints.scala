package io.tuliplogic.raytracer.http.model.attapirato


import java.util.UUID

import io.tuliplogic.raytracer.http.model.attapirato.drawings.Scenes
import io.tuliplogic.raytracer.http.model.attapirato.types.drawing.{DrawResponse, Scene, SceneDescription, SceneId}
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.APIError
import io.tuliplogic.raytracer.http.model.attapirato.types.user.Event.{LoginSuccess, PasswordUpdated, UserCreated}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.Cmd.{CreateUser, Login, UpdatePassword}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.UserId
import io.tuliplogic.raytracer.http.model.attapirato.users.Users
import org.http4s.HttpRoutes
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.logging.Logging
import zio.{RIO, Task, URIO, ZIO}

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

  val renderScene: Endpoint[SceneDescription, APIError, DrawResponse, Nothing] =
    endpoint.post.in("scene").in(jsonBody[SceneDescription]).out(jsonBody[DrawResponse]).errorOut(jsonBody[APIError])
      .description("Draw an image from a given Scene description")

  val getAllScenes: Endpoint[String, APIError, List[Scene], Nothing] =
    endpoint.get.in("scene" / path[String]("sceneId")).out(jsonBody[List[Scene]]).errorOut(jsonBody[APIError])
      .description("Fetch all the scene descriptions for the authenticated user")

  val getScene: Endpoint[String, APIError, Scene, Nothing] =
    endpoint.get.in("scene" / path[String]("sceneId"))
      .out(jsonBody[Scene]).errorOut(jsonBody[APIError])
      .description("Fetch all a single scene descriptions for the authenticated user")

  val getSceneImage: Endpoint[String, APIError, Array[Byte], Nothing] =
    endpoint.get.in("scene" / path[String]("sceneId") / "png")
      .out(byteArrayBody).errorOut(jsonBody[APIError])
      .description("Fetch the image of a single scene")

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

  object scenes {
    val triggerRendering: ZServerEndpoint[Scenes, SceneDescription, APIError, DrawResponse] =
      endpoints.renderScene.zServerLogic(sceneDescription =>
        Scenes.createScene(UserId(UUID.fromString("91171a5e-1376-4fc4-8929-b6e2654f5014")), sceneDescription)
          .map(scene => DrawResponse(scene.id, scene.status))
      )

    val getScene =
      endpoints.getScene.zServerLogic { sceneId =>
            Scenes.getScene(UserId(UUID.fromString("91171a5e-1376-4fc4-8929-b6e2654f5014")), SceneId(UUID.fromString(sceneId)))
      }

    val getSceneImage =
      endpoints.getSceneImage.zServerLogic { sceneId =>
        Scenes.getSceneImage(UserId(UUID.fromString("91171a5e-1376-4fc4-8929-b6e2654f5014")), SceneId(UUID.fromString(sceneId)))
      }
  }

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

    val createUser: URIO[Users, HttpRoutes[Task]] = zioEndpoints.user.createUser.toRoutesR
    val updateUserPwd: URIO[Users, HttpRoutes[Task]] = zioEndpoints.user.updatePassword.toRoutesR
    val loginUser: URIO[Users, HttpRoutes[Task]] = zioEndpoints.user.login.toRoutesR
    val triggerRendering: URIO[Scenes, HttpRoutes[Task]] = zioEndpoints.scenes.triggerRendering.toRoutesR
    val getScene: URIO[Scenes, HttpRoutes[Task]] = zioEndpoints.scenes.getScene.toRoutesR
    val getSceneImage: URIO[Scenes, HttpRoutes[Task]] = zioEndpoints.scenes.getSceneImage.toRoutesR

    val openApiDocs: OpenAPI = Seq(
      endpoints.createUser,
      endpoints.updatePassword,
      endpoints.login,
      endpoints.renderScene,
      endpoints.getScene,
      endpoints.getSceneImage,
    ).toOpenAPI("Ray Tracing as a Service", "1.0")
      .servers(List(Server("localhost:8090").description("local server")))

    val docsRoutes: HttpRoutes[Task] = new SwaggerHttp4s(openApiDocs.toYaml).routes[Task]

    val allRoutes: List[URIO[Users with Scenes with Logging, HttpRoutes[Task]]] = List(createUser, updateUserPwd, loginUser, triggerRendering, getScene, getSceneImage)

  val serve: RIO[Users with Scenes with Logging, Unit] = for {
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
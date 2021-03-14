package io.tuliplogic.raytracer.http

import java.util.UUID
import eu.timepit.refined.types.string.NonEmptyString
import io.tuliplogic.raytracer.http.drawings.Scenes
import io.tuliplogic.raytracer.http.types.drawing.{DrawResponse, Scene, SceneDescription, SceneId}
import io.tuliplogic.raytracer.http.types.AppError.APIError
import io.tuliplogic.raytracer.http.types.user.Event.{LoginSuccess, PasswordUpdated, UserCreated}
import io.tuliplogic.raytracer.http.types.user.Cmd.{CreateUser, Login, UpdatePassword}
import io.tuliplogic.raytracer.http.types.user.AccessToken
import io.tuliplogic.raytracer.http.users.Users
import org.http4s.HttpRoutes
import sttp.tapir.openapi.OpenAPI
import sttp.tapir.swagger.http4s.SwaggerHttp4s
import zio.clock.Clock
import zio.logging.Logging
import zio.{RIO, UIO, ZIO}

object endpoints {

  import sttp.tapir.ztapir._
  import sttp.tapir.generic.auto._
  import sttp.tapir.json.circe._
  import sttp.tapir.codec.newtype._
  import sttp.tapir.codec.refined._
  import io.circe.generic.auto._
  import io.circe.refined._

  val authBearer = auth.bearer[String]()
  val createUser: ZEndpoint[CreateUser, APIError, UserCreated] =
    endpoint.post.in("user").in(jsonBody[CreateUser]).out(jsonBody[UserCreated]).errorOut(jsonBody[APIError])
      .description("Create a user")

  val updatePassword: ZEndpoint[UpdatePassword, APIError, PasswordUpdated] =
    endpoint.put.in("user").in(jsonBody[UpdatePassword]).out(jsonBody[PasswordUpdated]).errorOut(jsonBody[APIError])
      .description("Update user password")


  val login: ZEndpoint[Login, APIError, LoginSuccess] =
    endpoint.post.in("login").in(jsonBody[Login]).out(jsonBody[LoginSuccess]).errorOut(jsonBody[APIError])
      .description("Login to obtain an access token")

  val renderScene: ZEndpoint[(SceneDescription, String), APIError, DrawResponse] =
    endpoint.post.in("scene").in(jsonBody[SceneDescription]).in(authBearer)
      .out(jsonBody[DrawResponse]).errorOut(jsonBody[APIError])
      .description("Draw an image from a given Scene description")

  val getAllScenes: ZEndpoint[String, APIError, List[Scene]] =
    endpoint.get.in("scene").in(authBearer)
      .out(jsonBody[List[Scene]]).errorOut(jsonBody[APIError])
      .description("Fetch all the scene descriptions for the authenticated user")

  val getScene: ZEndpoint[(String, String), APIError, Scene] =
    endpoint.get.in("scene" / path[String]("sceneId")).in(authBearer)
      .out(jsonBody[Scene]).errorOut(jsonBody[APIError])
      .description("Fetch all a single scene descriptions for the authenticated user")

  val getSceneImage: ZEndpoint[(String, String), APIError, Array[Byte]] =
    endpoint.get.in("scene" / path[String]("sceneId") / "png").in(authBearer)
      .out(byteArrayBody)
      .out(header("Content-Type", "image/png"))
      .errorOut(jsonBody[APIError])
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

    val triggerRendering: ZServerEndpoint[Scenes with Users, (SceneDescription, String), APIError, DrawResponse] =
      endpoints.renderScene.zServerLogic { case (sceneDescription, at) =>
        for {
          userId <- Users.authenticate(AccessToken(NonEmptyString.unsafeFrom(at)))
          scene  <- Scenes.createScene(userId, sceneDescription)
        } yield DrawResponse(scene.id, scene.status)
      }

    val getScene: ZServerEndpoint[Scenes with Users, (String, String), APIError, Scene] =
      endpoints.getScene.zServerLogic { case (sceneId, at) =>
        for {
          userId <- Users.authenticate(AccessToken((NonEmptyString.unsafeFrom(at))))
          scene <- Scenes.getScene(userId, SceneId(UUID.fromString(sceneId)))
        } yield scene
      }

    val getAllScenes: ZServerEndpoint[Scenes with Users, String, APIError, List[Scene]] =
      endpoints.getAllScenes.zServerLogic { case at =>
        for {
          userId <- Users.authenticate(AccessToken((NonEmptyString.unsafeFrom(at))))
          scene <- Scenes.getScenes(userId)
        } yield scene
      }

    val getSceneImage: ZServerEndpoint[Scenes with Users, (String, String), APIError, Array[Byte]] =
      endpoints.getSceneImage.zServerLogic { case (sceneId, at) =>
        for {
          userId <- Users.authenticate(AccessToken((NonEmptyString.unsafeFrom(at))))
          sceneBytes <- Scenes.getSceneImage(userId, SceneId(UUID.fromString(sceneId)))
        } yield sceneBytes
      }
  }

}





object AllRoutes {

  import zio.interop.catz._
  import zio.interop.catz.implicits._
  import sttp.tapir.server.http4s.ztapir.ZHttp4sServerInterpreter
  import sttp.tapir.ztapir._
  import org.http4s.syntax.kleisli._

  import org.http4s.server.Router
  import org.http4s.server.blaze.BlazeServerBuilder
  import sttp.tapir.docs.openapi._
  import sttp.tapir.openapi.Server
  import sttp.tapir.openapi.circe.yaml._
  import cats.implicits._

  val openApiDocs: OpenAPI = OpenAPIDocsInterpreter.toOpenAPI(
    List(
      endpoints.createUser,
      endpoints.updatePassword,
      endpoints.login,
      endpoints.renderScene,
      endpoints.getScene,
      endpoints.getAllScenes,
      endpoints.getSceneImage,
    ), "Ray Tracing as a Service", "1.0"
  ).servers(List(Server("http://localhost:8090").description("local server")))

  val docsRoutes = new SwaggerHttp4s(openApiDocs.toYaml)

  type MyEnv = Users with Scenes with Logging
  type F[A] = RIO[MyEnv, A]
  type G[A] = RIO[MyEnv with Clock, A]
  val allRoutes: HttpRoutes[G] = ZHttp4sServerInterpreter.from(
    List(
      zioEndpoints.user.createUser.widen[MyEnv],
      zioEndpoints.user.updatePassword.widen[MyEnv],
      zioEndpoints.user.login.widen[MyEnv],
      zioEndpoints.scenes.triggerRendering.widen[MyEnv],
      zioEndpoints.scenes.getScene.widen[MyEnv],
      zioEndpoints.scenes.getAllScenes.widen[MyEnv],
      zioEndpoints.scenes.getSceneImage.widen[MyEnv]
    )
  ).toRoutes

  val serve: G[Unit] = for {
      allRoutes <- UIO.succeed(allRoutes <+> docsRoutes.routes[G])
      _         <- serveRoutes(allRoutes)
    } yield ()

  def serveRoutes(rs: HttpRoutes[G]): G[Unit] = ZIO
      .runtime[MyEnv with Clock]
      .flatMap { implicit rts =>
        BlazeServerBuilder[G](rts.platform.executor.asEC)
          .bindHttp(8090, "localhost")
          .withHttpApp(Router("/"-> rs ).orNotFound)
          .serve
          .compile
          .drain
      }
}
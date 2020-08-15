package io.tuliplogic.raytracer.http.model.attapirato.drawings


import doobie.{Query0, Update0}
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.APIError
import io.tuliplogic.raytracer.http.model.attapirato.types.drawing.{Scene, SceneDescription, SceneId}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.UserId
import zio.{IO, ZIO}

object ScenesRepo {

  trait Service {
    def createScene(userId: UserId, sceneId: SceneId, scene: SceneDescription): IO[APIError, Scene]
    def saveScene(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): IO[APIError, Unit]
    def getScene(userId: UserId, sceneId: SceneId): IO[APIError, Scene]
    def getScenes(userId: UserId): IO[APIError, List[Scene]]
    def getSceneImage(userId: UserId, sceneId: SceneId): IO[APIError, Array[Byte]]
  }

  def createScene(userId: UserId, sceneId: SceneId, scene: SceneDescription): ZIO[ScenesRepo, APIError, Scene] =
    ZIO.accessM(_.get.createScene(userId, sceneId, scene))
  def saveScene(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): ZIO[ScenesRepo, APIError, Unit] =
    ZIO.accessM(_.get.saveScene(userId, sceneId, bytes))
  def getScene(userId: UserId, sceneId: SceneId): ZIO[ScenesRepo, APIError, Scene] =
  ZIO.accessM(_.get.getScene(userId, sceneId))
  def getScenes(userId: UserId): ZIO[ScenesRepo, APIError, List[Scene]] =
  ZIO.accessM(_.get.getScenes(userId))
  def getSceneImage(userId: UserId, sceneId: SceneId): ZIO[ScenesRepo, APIError, Array[Byte]] =
  ZIO.accessM(_.get.getSceneImage(userId, sceneId))





  object Queries {

    import doobie.implicits._
    import doobie.postgres.implicits._
    import io.tuliplogic.raytracer.http.model.attapirato.doobieUtils._

    import io.circe.generic.auto._
    import io.circe.syntax._

    def createScene(userId: UserId, sceneId: SceneId, scene: SceneDescription): Update0 =
      sql"""
           |insert into drawings (
           |'id, user_id, scene'
           |) VALUES (
           |${sceneId},
           |${userId},
           |${scene.asJson.spaces2},
           |)
           |""".stripMargin.update


    def saveScene(userId: UserId, sceneId: SceneId, png: Array[Byte]): Update0 =
      sql"""
           |update drawings (
           |set 'png' = ${png}
           |) where (
           |id = ${sceneId} and
           |user_id = ${userId},
           |)
           |""".stripMargin.update

    def getScene(userId: UserId, sceneId: SceneId): Query0[String] =
      sql"""select scene from drawings
           |  where id = ${sceneId}
           |  and user_id = ${userId}
         """.stripMargin.query[String]

    def getScenes(userId: UserId): Query0[String] =
      sql"""select scene from drawings
           |  where user_id = ${userId}
         """.stripMargin.query[String]

    def getSceneImage(userId: UserId, sceneId: SceneId): Query0[Array[Byte]] =
      sql"""select scene from drawings
           |  where id = ${sceneId}
           |  and user_id = ${userId}
         """.stripMargin.query[Array[Byte]]
  }
}

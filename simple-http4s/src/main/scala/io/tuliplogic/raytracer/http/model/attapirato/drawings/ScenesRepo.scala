package io.tuliplogic.raytracer.http.model.attapirato.drawings


import doobie.hikari.HikariTransactor
import doobie.{Query0, Update0}
import io.tuliplogic.raytracer.http.model.attapirato.DB
import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.DBError
import io.tuliplogic.raytracer.http.model.attapirato.types.drawing.{Scene, SceneDescription, SceneId}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.UserId
import zio.{IO, Task, URLayer, ZIO, ZLayer}

object ScenesRepo {

  trait Service {
    def createScene(userId: UserId, sceneId: SceneId, scene: SceneDescription): IO[DBError, Unit]
    def saveScene(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): IO[DBError, Unit]
    def getScene(userId: UserId, sceneId: SceneId): IO[DBError, Scene]
    def getScenes(userId: UserId): IO[DBError, List[Scene]]
    def getSceneImage(userId: UserId, sceneId: SceneId): IO[DBError, Array[Byte]]
  }

  def createScene(userId: UserId, sceneId: SceneId, scene: SceneDescription): ZIO[ScenesRepo, DBError, Unit] =
    ZIO.accessM(_.get.createScene(userId, sceneId, scene))
  def saveScene(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): ZIO[ScenesRepo, DBError, Unit] =
    ZIO.accessM(_.get.saveScene(userId, sceneId, bytes))
  def getScene(userId: UserId, sceneId: SceneId): ZIO[ScenesRepo, DBError, Scene] =
  ZIO.accessM(_.get.getScene(userId, sceneId))
  def getScenes(userId: UserId): ZIO[ScenesRepo, DBError, List[Scene]] =
  ZIO.accessM(_.get.getScenes(userId))
  def getSceneImage(userId: UserId, sceneId: SceneId): ZIO[ScenesRepo, DBError, Array[Byte]] =
  ZIO.accessM(_.get.getSceneImage(userId, sceneId))


  val doobieLive: URLayer[DB.Transactor, ScenesRepo] =
    ZLayer.fromService[HikariTransactor[Task], ScenesRepo.Service] { transactor =>
      new Service {
        import doobie.implicits._
        import zio.interop.catz._
        import io.circe.parser._
        import io.circe.generic.auto._

        override def createScene(userId: UserId, sceneId: SceneId, scene: SceneDescription): IO[DBError, Unit] =
          Queries.createScene(userId, sceneId, scene)
            .run.transact(transactor)
            .mapError(e => DBError(s"Could not create scene for user $userId", Some(e)))
            .unit

        override def saveScene(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): IO[DBError, Unit] =
          Queries.saveScene(userId, sceneId, bytes)
            .run.transact(transactor)
            .mapError(e => DBError(s"Could not save scene $sceneId for user $userId", Some(e)))
            .unit

        override def getScene(userId: UserId, sceneId: SceneId): IO[DBError, Scene] =
          Queries.getScene(userId, sceneId)
            .unique
            .transact(transactor)
            .mapError(e => DBError(s"Could not get scene $sceneId for user $userId", Some(e)))
            .flatMap { s =>
              ZIO.fromEither(parse(s).flatMap(json => json.as[Scene]))
                .mapError(e => DBError(s"Could not scene description returned from DB for scene $sceneId for user $userId", Some(e)))
            }

        override def getScenes(userId: UserId): IO[DBError, List[Scene]] =
          Queries.getScenes(userId)
            .to[List]
            .transact(transactor)
            .mapError(e => DBError(s"Could not get scenes for user $userId", Some(e)))
            .flatMap { parseScenes }

        def parseScenes(xs: List[String]): IO[DBError, List[Scene]] =
          ZIO.foreach(xs)(s => ZIO.fromEither
            (parse(s).flatMap(json => json.as[Scene]))
              .mapError(e => DBError(s"Could not decode scene description", Some(e))))

        override def getSceneImage(userId: UserId, sceneId: SceneId): IO[DBError, Array[Byte]] =
          Queries.getSceneImage(userId, sceneId)
            .unique
            .transact(transactor)
            .mapError(e => DBError(s"Could not get scene images for user $userId", Some(e)))
      }
    }


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

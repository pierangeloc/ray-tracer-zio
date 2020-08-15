package io.tuliplogic.raytracer.http.model.attapirato.drawings

import java.util.UUID

import io.tuliplogic.raytracer.http.model.attapirato.types.AppError.APIError
import io.tuliplogic.raytracer.http.model.attapirato.types.drawing.{Scene, SceneDescription, SceneId, SceneStatus}
import io.tuliplogic.raytracer.http.model.attapirato.types.user.UserId
import zio.logging.{Logger, Logging}
import zio.{Has, IO, UIO, URLayer, ZIO, ZLayer}

object Scenes {

  trait Service {
    def createScene(userId: UserId, scene: SceneDescription): IO[APIError, Scene]
    def saveSceneImage(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): IO[APIError, Unit]
    def getScene(userId: UserId, sceneId: SceneId): IO[APIError, Scene]
    def getScenes(userId: UserId): IO[APIError, List[Scene]]
    def getSceneImage(userId: UserId, sceneId: SceneId): IO[APIError, Array[Byte]]
  }

  def createScene(userId: UserId, scene: SceneDescription): ZIO[Scenes, APIError, Scene] =
    ZIO.accessM(_.get.createScene(userId, scene))

  def saveScene(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): ZIO[Scenes, APIError, Unit] =
    ZIO.accessM(_.get.saveSceneImage(userId, sceneId, bytes))

  def getScene(userId: UserId, sceneId: SceneId): ZIO[Scenes, APIError, Scene] =
    ZIO.accessM(_.get.getScene(userId, sceneId))

  def getScenes(userId: UserId): ZIO[Scenes, APIError, List[Scene]] =
    ZIO.accessM(_.get.getScenes(userId))

  def getSceneImage(userId: UserId, sceneId: SceneId): ZIO[Scenes, APIError, Array[Byte]] =
    ZIO.accessM(_.get.getSceneImage(userId, sceneId))

  val live: URLayer[ScenesRepo with Logging, Has[Service]] =
    ZLayer.fromServices[ScenesRepo.Service, Logger[String], Service] { (scenesRepo, logger) =>
      new Service {
        def createScene(userId: UserId, scene: SceneDescription): IO[APIError, Scene] =
          UIO.effectTotal(UUID.randomUUID()).flatMap { id =>
            {
              val sceneId = SceneId(id.toString)
              scenesRepo
                .createScene(userId, sceneId, scene)
                .catchAll { e =>
                  logger.throwable(s"Error creating scene", e) *> ZIO.fail(APIError("Error creating user")),
                }
                .as(Scene(sceneId, scene, SceneStatus.InProgress))
            }
          }

        def saveSceneImage(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): IO[APIError, Unit] =
          scenesRepo
            .saveScene(userId, sceneId, bytes)
            .catchAll { e =>
              logger.throwable(s"Error saving scene image", e) *> ZIO.fail(APIError("Error saving scene image")),
            }

        def getScene(userId: UserId, sceneId: SceneId): IO[APIError, Scene] =
          scenesRepo.getScene(userId, sceneId)
            .catchAll { e =>
              logger.throwable(s"Error retrieving scene description", e) *> ZIO.fail(APIError("Error retrieving scene description")),
            }

        def getScenes(userId: UserId): IO[APIError, List[Scene]] =
          scenesRepo.getScenes(userId)
            .catchAll { e =>
              logger.throwable(s"Error retrieving all scene descriptions", e) *> ZIO.fail(APIError("Error retrieving scene description")),
            }

        def getSceneImage(userId: UserId, sceneId: SceneId): IO[APIError, Array[Byte]] =
          scenesRepo.getSceneImage(userId, sceneId)
            .catchAll { e =>
              logger.throwable(s"Error retrieving scene image", e) *> ZIO.fail(APIError("Error retrieving scene description")),
            }
      }

    }
}

package io.tuliplogic.raytracer.http.drawings

import java.util.UUID

import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.http.types
import io.tuliplogic.raytracer.http.types.AppError.APIError
import io.tuliplogic.raytracer.http.types.drawing.{Scene, SceneDescription, SceneId, SceneStatus}
import io.tuliplogic.raytracer.http.types.user.UserId
import zio.logging.{Logger, Logging, log}
import zio.{Has, IO, UIO, URLayer, ZIO, ZLayer}

object Scenes {

  trait Service {
    def createScene(userId: UserId, sceneDescription: SceneDescription): IO[APIError, Scene]
//    def saveSceneImage(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): IO[APIError, Unit]
    def getScene(userId: UserId, sceneId: SceneId): IO[APIError, Scene]
    def getScenes(userId: UserId): IO[APIError, List[Scene]]
    def getSceneImage(userId: UserId, sceneId: SceneId): IO[APIError, Array[Byte]]
  }

  def createScene(userId: UserId, sceneDescription: SceneDescription): ZIO[Scenes, APIError, Scene] =
    ZIO.accessM(_.get.createScene(userId, sceneDescription))

//  def saveScene(userId: UserId, sceneId: SceneId, bytes: Array[Byte]): ZIO[Scenes, APIError, Unit] =
//    ZIO.accessM(_.get.saveSceneImage(userId, sceneId, bytes))

  def getScene(userId: UserId, sceneId: SceneId): ZIO[Scenes, APIError, Scene] =
    ZIO.accessM(_.get.getScene(userId, sceneId))

  def getScenes(userId: UserId): ZIO[Scenes, APIError, List[Scene]] =
    ZIO.accessM(_.get.getScenes(userId))

  def getSceneImage(userId: UserId, sceneId: SceneId): ZIO[Scenes, APIError, Array[Byte]] =
    ZIO.accessM(_.get.getSceneImage(userId, sceneId))

  val live: URLayer[ScenesRepo with PngRenderer with ATModule with Logging, Scenes] =
    ZLayer.fromServices[ScenesRepo.Service, PngRenderer.Service, aTModule.Service, Logger[String], Service] { (scenesRepo, renderer, atModule, logger) =>
      new Service {

        def renderAndSaveWhenReady(userId: UserId, sceneId: SceneId, sceneDescription: SceneDescription): ZIO[Logging, types.AppError, Unit] =
          for {
            sceneBundle <- SceneDescriptionToSceneBundle.sceneDescription2SceneBundle(sceneDescription).provideSome[Logging](_ ++ Has(atModule))
            bytes <- renderer.draw(sceneBundle)
            _ <- log.info(s"image has been computed, now saving ${bytes.size} bytes to DB")
            _ <- scenesRepo.saveScene(userId, sceneId, bytes.toArray)
            _ <- log.info(s"image for scene $sceneId saved in DB" )
          } yield ()

        def createScene(userId: UserId, sceneDescription: SceneDescription): IO[APIError, Scene] = {
          (for {
            id <- UIO.effectTotal(UUID.randomUUID())
            sceneId = SceneId(id)
            _ <- scenesRepo.createScene(userId, sceneId, sceneDescription)
            _ <- renderAndSaveWhenReady(userId, sceneId, sceneDescription).provide(Has(logger)).forkDaemon
          } yield Scene(sceneId, sceneDescription, SceneStatus.InProgress))
            .catchAll { e =>
            logger.throwable(s"Error starting scene computation", e) *> ZIO.fail(APIError("Error starting scene computation"))
          }
        }

        def getScene(userId: UserId, sceneId: SceneId): IO[APIError, Scene] =
          scenesRepo.getScene(userId, sceneId)
            .catchAll { e =>
              logger.throwable(s"Error retrieving scene description", e) *> ZIO.fail(APIError("Error retrieving scene description"))
            }

        def getScenes(userId: UserId): IO[APIError, List[Scene]] =
          scenesRepo.getScenes(userId)
            .catchAll { e =>
              logger.throwable(s"Error retrieving all scene descriptions", e) *> ZIO.fail(APIError("Error retrieving scene description"))
            }

        def getSceneImage(userId: UserId, sceneId: SceneId): IO[APIError, Array[Byte]] =
          scenesRepo.getSceneImage(userId, sceneId)
            .catchAll { e =>
              logger.throwable(s"Error retrieving scene image", e) *> ZIO.fail(APIError("Error retrieving scene description"))
            }
      }

    }
}

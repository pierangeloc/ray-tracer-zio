package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.http.model.DrawingProgram.DrawEnv
import io.tuliplogic.raytracer.http.model.DrawingRepoModel.{DrawingId, DrawingState}
import io.tuliplogic.raytracer.ops.programs.FullModules
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import io.tuliplogic.repository.UserRepository
import zio.clock.Clock
import zio.console._
import zio.random.Random
import zio.{App, RIO, Ref, UIO, ZEnv, ZIO}

/**
  *
  * zio-cases - 2019-06-06
  * Created with ♥ in Amsterdam
  */
object Main extends App {
  type AppEnvironment = Clock with UserRepository
  type AppTask[A]     = RIO[AppEnvironment, A]

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] = {
    (for {
      imageRepoRef <- Ref.make(Map[DrawingId, DrawingState]())
      _            <- HttpServer.make.serve.provide {
                        new Clock.Live
                        with Console.Live
                        with Random.Live
                        with FullModules
                        with CanvasSerializer.PNGCanvasSerializer
                        with DrawingRepository {
                          val drawingRepository: DrawingRepository.Service[Any] =
                            DrawingRepository.RefDrawingRepoService(imageRepoRef)
                        }
                      }
    } yield ()).foldM(
      err => putStr(s"Error running application $err") *> ZIO.succeed(1),
      _ => ZIO.succeed(0))

  }

}

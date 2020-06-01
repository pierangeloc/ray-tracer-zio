package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.http.model.DrawingRepoModel.{DrawingId, DrawingState}
import io.tuliplogic.raytracer.ops.programs.layers
import zio.clock.Clock
import zio.console._
import zio.random.Random
import zio.{App, ExitCode, Ref, ZEnv, ZIO, ZLayer}

/**
  *
  * zio-cases - 2019-06-06
  * Created with ♥ in Amsterdam
  */
object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    (for {
      imageRepoRef <- Ref.make(Map[DrawingId, DrawingState]())
      drawingRepo  = drawingRepository.refDrawingRepoService(imageRepoRef)
      _            <- HttpServer.make.serve.provideLayer {
                        ZLayer.identity[Clock with Console with Random] ++
                          (layers.atM >>> layers.rasteringM) ++
                          layers.atM++
                        layers.cSerializerM ++
                        drawingRepo
                      }
    } yield ()).foldM(
      err => putStr(s"Error running application $err") *> ZIO.succeed(ExitCode.failure),
      _ => ZIO.succeed(ExitCode.success))

  }

}

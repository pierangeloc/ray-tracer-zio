package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.model.data
import io.tuliplogic.raytracer.ops.model.data.{Camera, Color, ColoredPixel, Pixel, World}
import io.tuliplogic.raytracer.ops.model.modules.cameraModule.CameraModule
import io.tuliplogic.raytracer.ops.model.modules.worldModule.WorldModule
import zio.stream.{ZStream, ZStreamChunk}
import zio.{Chunk, Has, Ref, ZIO, ZLayer}


//TODO: PERFORMANCE IMPROVEMENTS:
// start from chunks, not from pixels and then chunked
// in the phong reflection model pass chunks, but not a chunk of (HitComps, Shadowed) but a set of chunks for each component of
// the hitComps and shadowed. This should boost the performance by 2-3 orders
/**
  * This module provides access to a stream of colored point, given a world and a camera
  */
object rasteringModule {

  val defaultRemaining = 12
  trait Service {
    def raster(world: World, camera: Camera): ZStream[Any, RayTracerError, ColoredPixel]
  }

  type RasteringModule = Has[Service]
  /**
    * This implementation tries to exploit the parallelism as much as possible, splitting the stream in chunks and processing each chunk of pixels in parallel
    */

  val chunkSize: Int = 4096
  val parChunks: Int = 7//15 //nr cores - 1

  val chunkRasteringModule: ZLayer[CameraModule with WorldModule, Nothing, RasteringModule] =
    ZLayer.fromServices[cameraModule.Service, worldModule.Service, rasteringModule.Service] {
    (cameraSvc, worldSvc) =>
      new Service {
        override def raster(world: World, camera: Camera): ZStream[Any, RayTracerError, ColoredPixel] = {

          val pixels: Array[(Int, Int)] = for {
            x <- (0 until camera.hRes).toArray
            y <- (0 until camera.vRes).toArray
          } yield (x, y)

          ZStreamChunk.fromChunks(pixels.grouped(chunkSize).map(Chunk.fromArray).toList: _*).chunks.mapMPar(parChunks) { chunk =>
            chunk.mapM {
              case (px, py) =>
                for {
                  remaining <- Ref.make(defaultRemaining)
                  ray <- cameraSvc.rayForPixel(camera, px, py)
                  color <- worldSvc.colorForRay(world, ray, remaining)
                } yield data.ColoredPixel(Pixel(px, py), color)
            }
          }.flatMap(ZStream.fromChunk(_))
        }
      }
  }


  val allWhiteTestRasteringModule: ZLayer.NoDeps[Nothing, RasteringModule] = ZLayer.succeed {
    new Service {
      override def raster(world: World, camera: Camera): ZStream[Any, RayTracerError, ColoredPixel] =
        for {
          x <- ZStream.fromIterable(0 until camera.hRes)
          y <- ZStream.fromIterable(0 until camera.vRes)
        } yield ColoredPixel(Pixel(x, y), Color.white)
    }
  }

  def raster(world: World, camera: Camera): ZStream[RasteringModule, RayTracerError, ColoredPixel] =
    ZStream.fromEffect(
        ZIO.access[RasteringModule](r => r.get.raster(world, camera))).flatMap(identity)
}

package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.model.data.{Camera, Color, ColoredPixel, Pixel, World}
import zio.stream.ZStream
import zio.{UIO, ZIO}

/**
  * This module provides access to a stream of colored point, given a world and a camera
  */
trait RasteringModule {
  val rasteringModule: RasteringModule.Service[Any]
}

object RasteringModule {

  trait Service[R] {
    def raster(world: World, camera: Camera): ZIO[R, Nothing, ZStream[R, RayTracerError, ColoredPixel]]
  }

  /**
    * This implementation tries to exploit the parallelism as much as possible, splitting the stream in chunks and processing each chunk of pixels in parallel
    */
  trait ChunkRasteringModule extends RasteringModule {
    val chunkSize: Int = 4096
    val parChunks: Int = 15 //nr cores - 1
    val cameraModule: CameraModule.Service[Any]
    val worldModule: WorldModule.Service[Any]

    override val rasteringModule: Service[Any] = new Service[Any] {
      override def raster(world: World, camera: Camera): UIO[ZStream[Any, RayTracerError, ColoredPixel]] = {
        val pixels = for {
          x <- ZStream.fromIterable(0 until camera.hRes )
          y <- ZStream.fromIterable(0 until camera.vRes )
        } yield (x, y)

        UIO.succeed(pixels.chunkN(4096).mapMPar(parChunks) { chunk =>
          chunk.mapM {
            case (px, py) =>
              for {
                ray   <- cameraModule.rayForPixel(camera, px, py)
                color <- worldModule.colorForRay(world, ray)
              } yield data.ColoredPixel(Pixel(px, py), color)
          }
        }.flatMap(ZStream.fromChunk))
      }
    }
  }

  trait AllWhiteTestRasteringModule extends RasteringModule {
    override val rasteringModule: Service[Any] = new Service[Any] {
      override def raster(world: World, camera: Camera): UIO[ZStream[Any, RayTracerError, ColoredPixel]] =
        UIO.succeed(for {
          x <- ZStream.fromIterable(0 until camera.hRes)
          y <- ZStream.fromIterable(0 until camera.vRes)
        } yield ColoredPixel(Pixel(x, y), Color.white))
    }
  }

  object > extends RasteringModule.Service[RasteringModule] {
    override def raster(world: World, camera: Camera): ZIO[RasteringModule, Nothing, ZStream[RasteringModule, RayTracerError, ColoredPixel]] =
          ZIO.accessM[RasteringModule](_.rasteringModule.raster(world, camera))
  }
}

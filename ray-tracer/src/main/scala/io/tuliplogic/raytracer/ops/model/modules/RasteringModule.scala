package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.ops.model.data
import io.tuliplogic.raytracer.ops.model.data.{Camera, Color, ColoredPixel, Pixel, World}
import zio.stream.{ZStream, ZStreamChunk}
import zio.{Chunk, Ref, UIO, ZIO}


//TODO: PERFORMANCE IMPROVEMENTS:
// start from chunks, not from pixels and then chunked
// in the phong reflection model pass chunks, but not a chunk of (HitComps, Shadowed) but a set of chunks for each component of
// the hitComps and shadowed. This should boost the performance by 2-3 orders
/**
  * This module provides access to a stream of colored point, given a world and a camera
  */
trait RasteringModule {
  val rasteringModule: RasteringModule.Service[Any]
}

object RasteringModule {

  val defaultRemaining = 7
  trait Service[R] {
    def raster(world: World, camera: Camera): ZStream[R, RayTracerError, ColoredPixel]
//    ZIO[R, Nothing, ZStream[R, RayTracerError, ColoredPixel]]
  }

  /**
    * This implementation tries to exploit the parallelism as much as possible, splitting the stream in chunks and processing each chunk of pixels in parallel
    */
  trait ChunkRasteringModule extends RasteringModule {
    val chunkSize: Int = 4096
    val parChunks: Int = 7//15 //nr cores - 1
    val cameraModule: CameraModule.Service[Any]
    val worldModule: WorldModule.Service[Any]

    override val rasteringModule: Service[Any] = new Service[Any] {
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
                ray   <- cameraModule.rayForPixel(camera, px, py)
                color <- worldModule.colorForRay(world, ray, remaining)
              } yield data.ColoredPixel(Pixel(px, py), color)
          }
        }.flatMap(ZStream.fromChunk)
      }
    }
  }

  trait SlowRasteringModule extends RasteringModule {
    val cameraModule: CameraModule.Service[Any]
    val worldModule: WorldModule.Service[Any]

    override val rasteringModule: Service[Any] = new Service[Any] {
      override def raster(world: World, camera: Camera): ZStream[Any, RayTracerError, ColoredPixel] = {
        val pixels: zio.stream.Stream[Nothing, (Int, Int)] = for {
          x <- ZStream.fromIterable(0 until camera.hRes )
          y <- ZStream.fromIterable(0 until camera.vRes )
        } yield (x, y)

        pixels.mapM{
          case (px, py) =>
            for {
              remaining <- Ref.make(defaultRemaining)
              ray   <- cameraModule.rayForPixel(camera, px, py)
              color <- worldModule.colorForRay(world, ray, remaining)
            } yield data.ColoredPixel(Pixel(px, py), color)
        }
      }
    }
  }

  trait AllWhiteTestRasteringModule extends RasteringModule {
    override val rasteringModule: Service[Any] = new Service[Any] {
      override def raster(world: World, camera: Camera): ZStream[Any, RayTracerError, ColoredPixel] =
        for {
          x <- ZStream.fromIterable(0 until camera.hRes)
          y <- ZStream.fromIterable(0 until camera.vRes)
        } yield ColoredPixel(Pixel(x, y), Color.white)
    }
  }

  object > extends RasteringModule.Service[RasteringModule] {
    override def raster(world: World, camera: Camera): ZStream[RasteringModule, RayTracerError, ColoredPixel] =
      ZStream.fromEffect(
          ZIO.access[RasteringModule](r => r.rasteringModule.raster(world, camera))).flatMap(identity)
  }
}

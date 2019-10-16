package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformationOps
import io.tuliplogic.raytracer.ops.model.{Color, PhongReflection, RayOperations, SpatialEntityOperations}
import zio.Chunk
import zio.stream.{Stream, StreamChunk, ZStreamChunk}

import scala.{Stream => ScalaStream}

object Renderer {
  type UStreamC[A] = StreamChunk[Nothing, A]

  def render(camera: Camera, world: World): ZStreamChunk[
    PhongReflection with SpatialEntityOperations with RayOperations with AffineTransformationOps,
    RayTracerError,
    (Int, Int, Color)] =
    pixelsChunkedStream(camera).mapM {
      case (px, py) =>
        for {
          ray   <- camera.rayForPixel(px, py)
          color <- world.colorAt(ray)
        } yield (px, py, color)
    }

  private def pixels(chunkSize: Int)(camera: Camera): scala.Stream[Chunk[(Int, Int)]] =
    (for {
      px <- ScalaStream.from(0).take(camera.hRes)
      py <- ScalaStream.from(0).take(camera.vRes)
    } yield (px, py))
      .grouped(chunkSize)
      .map(str => Chunk.fromIterable(str))
      .toStream

  private def pixelsChunkedStream: Camera => UStreamC[(Int, Int)] = c => StreamChunk(Stream.fromIterable(pixels(4096)(c)))

}

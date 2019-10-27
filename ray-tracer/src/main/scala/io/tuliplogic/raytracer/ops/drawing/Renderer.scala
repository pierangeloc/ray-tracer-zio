package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.ops.model.{CameraModule, Color, NormalReflectModule, PhongReflectionModule, RayModule, WorldModule}
import zio.Chunk
import zio.stream.{Stream, StreamChunk, ZStreamChunk}

import scala.{Stream => ScalaStream}

object Renderer {
  type UStreamC[A] = StreamChunk[Nothing, A]

  def render(camera: Camera, world: World): ZStreamChunk[
    WorldModule with CameraModule,
    Nothing,
    (Int, Int, Color)] =
    pixelsChunkedStream(camera).mapM {
      case (px, py) =>
        (for {
          ray   <- CameraModule.>.rayForPixel(camera, px, py)
          color <- WorldModule.>.colorForRay(world, ray)
        } yield (px, py, color)).orDie
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

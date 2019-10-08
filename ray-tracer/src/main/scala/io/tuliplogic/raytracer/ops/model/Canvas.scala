package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.commons.errors.CanvasError.IndexExceedCanvasDimension
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import zio.{Chunk, IO, UIO, ZIO}
import zio.stream._

import scala.{Stream => ScalaStream}

abstract class Canvas(private val width_ : Int, height_ : Int, rows: Array[Array[Color]]) {
  private def checkAccessIndex(x: Int, y: Int): IO[IndexExceedCanvasDimension, Unit] =
    if (x >= 0 && x < width_ && y >= 0 && y < height_) IO.unit
    else IO.fail(IndexExceedCanvasDimension(x, y, width_, height_))

  def width: UIO[Int]  = UIO.succeed(width_)
  def height: UIO[Int] = UIO.succeed(height_)
  def get(x: Int, y: Int): IO[IndexExceedCanvasDimension, Color] =
    checkAccessIndex(x, y) *> UIO.succeed(rows(y)(x))

  def update(x: Int, y: Int, color: Color, failOutOfBound: Boolean = false): IO[IndexExceedCanvasDimension, Unit] =
    checkAccessIndex(x, y).foldM(
      exc => if (failOutOfBound) ZIO.fail(exc) else ZIO.succeed(()),
      _ =>
        UIO.effectTotal {
          val colI = rows(y)
          colI.update(x, color)
      }
    )

  def rows: UIO[Array[Array[Color]]] =
    UIO.succeed(rows)
}

object Canvas {
  def create(width: Int, height: Int): UIO[Canvas] = UIO.effectTotal {
    new Canvas(width, height, Array.fill(height)(Array.fill(width)(Color.black))) {}
  }

  //TODO: make a `SampledRect` case class with this info
  def canvasPixels(halfWidth: Double, halfHeight: Double, z: Double, hres: Int, vres: Int, chunkSize: Int): ScalaStream[Chunk[(Pt, Int, Int)]] =
    (for {
      xn <- ScalaStream.from(0).take(hres)
      yn <- ScalaStream.from(0).take(vres)
    } yield {
      val cX = xn * (halfWidth * 2) / hres
      val cY = yn * (halfHeight * 2) / vres
      (Pt(cX - halfWidth, -cY + halfHeight, z), xn, yn)
    }).grouped(chunkSize)
      .map(str => Chunk.fromIterable(str))
      .toStream

  def canvasPixelsChunked(halfWidth: Double, halfHeight: Double, z: Double, hres: Int, vres: Int) =
    StreamChunk(Stream.fromIterable(canvasPixels(halfWidth, halfHeight, z, hres, vres, 4096)))

}

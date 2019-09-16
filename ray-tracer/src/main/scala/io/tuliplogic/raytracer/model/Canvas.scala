package io.tuliplogic.raytracer.model

import io.tuliplogic.raytracer.errors.CanvasError.IndexExceedCanvasDimension
import zio.{IO, UIO}


abstract class Canvas(private val width_ : Int, height_ : Int, rows: Array[Array[Color]]) {
  private def checkAccessIndex(x: Int, y: Int): IO[IndexExceedCanvasDimension, Unit] =
    if (x >= 0 && x < width_ && y >= 0 && y < height_) IO.unit
    else IO.fail(IndexExceedCanvasDimension(x, y, width_, height_))

  def width: UIO[Int] = UIO.succeed(width_)
  def height: UIO[Int] = UIO.succeed(height_)
  def get(x: Int, y: Int): IO[IndexExceedCanvasDimension, Color] =
    checkAccessIndex(x, y) *> UIO.succeed(rows(x)(y))

  def update(x: Int, y: Int, color: Color): IO[IndexExceedCanvasDimension, Unit] =
    checkAccessIndex(x, y) *> UIO.effectTotal {
      val colI = rows(x)
      colI.update(y, color)
    }
}

object Canvas {
  def create(width: Int, height: Int): UIO[Canvas] = UIO.effectTotal {
    new Canvas(width, height, Array.fill(width)(Array.fill(height)(Color.black))){}
  }
}

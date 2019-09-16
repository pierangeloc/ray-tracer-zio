package io.tuliplogic.raytracer.canvas

import cats.Show
import io.tuliplogic.raytracer.errors.CanvasError.IndexExceedCanvasDimension
import zio._

object model {

  case class Pixel(x: Int, y: Int)
  case class Color(red: Double, green: Double, blue: Double) //TODO: use refined types to enforce something on these colors

  object Color {
    val black = Color(0, 0, 0)
    def add(c1: Color, c2: Color): Color = Color(c1.red + c2.red, c1.green + c2.green, c1.blue + c2.blue)
    def mul(c1: Color, c2: Color): Color = Color(c1.red * c2.red, c1.green * c2.green, c1.blue * c2.blue)
  }

  final case class ColoredPoint(pixel: Pixel, color: Color) {
    override def toString: String = s"(${pixel.x}, ${pixel.y}, [${color.red}, ${color.green}, ${color.blue}])"
  }

  abstract class Canvas(width: Int, height: Int) {
    val rows: Chunk[Chunk[Color]]

    private def checkAccessIndex(i: Int, j: Int): IO[IndexExceedCanvasDimension, Unit] =
      if (i >= 0 && i < width && j >= 0 && j < height) IO.unit
      else IO.fail(IndexExceedCanvasDimension(i, j, width, height))

    def get(i: Int, j: Int): IO[IndexExceedCanvasDimension, Color] =
      checkAccessIndex(i, j) *> UIO.succeed(rows.toArray.apply(i).toArray.apply(j))

    def update(x: Int, y, color: Color): IO[IndexExceedCanvasDimension, Unit] =
      checkAccessIndex(x, y) *> UIO.effectTotal{
        val colI = rows.toArray.apply(x)
        colI.toArray.update(y, color)
      }
  }

  object Canvas {
    def create(width: Int, height: Int): Canvas = new Canvas(width, height) {
      override val rows: Chunk[Chunk[Color]] = Chunk.fromArray(Array.fill(height)(Chunk.fromArray(Array.fill(width)(Color.black))))
    }


  }
}

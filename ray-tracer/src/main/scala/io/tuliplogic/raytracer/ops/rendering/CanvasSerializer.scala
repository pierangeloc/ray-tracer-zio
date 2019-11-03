package io.tuliplogic.raytracer.ops.rendering

import java.nio.file.{Path, StandardOpenOption}

import io.tuliplogic.raytracer.commons.errors.IOError
import io.tuliplogic.raytracer.ops.model.{Canvas, Color}
import zio.nio.channels.AsynchronousFileChannel
import zio.stream._
import zio.{Chunk, UIO, ZIO}
import mouse.all._
import zio.blocking.Blocking

import scala.math.min
//TODO: rename this package "serializing"
trait CanvasSerializer {
  def canvasSerializer: CanvasSerializer.Service[Any]
}

object CanvasSerializer {
  trait Service[R] {
    def render(canvas: Canvas, maxColor: Int): ZIO[R, IOError, Unit]
  }

  trait PPMCanvasSerializer extends CanvasSerializer with Blocking { self =>
    def path: Path
    override def canvasSerializer: Service[Any] = new Service[Any] {

      def formatHeader: String                  = "P3"
      def sizeHeader(c: Canvas): UIO[String]    = c.width.zip(c.height).map { case (w, h) => s"$w $h" }
      def maxColorHeader(maxColor: Int): String = maxColor.toString

      def headers(c: Canvas, maxColor: Int): UIO[String] =
        for {
          sizeH <- sizeHeader(c)
        } yield s"""$formatHeader
                         |$sizeH
                         |${maxColorHeader(maxColor)}
                         |""".stripMargin

      def rowToString(row: Array[Color], maxColor: Int): Chunk[String] =
        Chunk.fromArray(row).map {
          case Color(r, g, b) =>
            val cappedR = min((r * maxColor).toInt, maxColor)
            val cappedG = min((g * maxColor).toInt, maxColor)
            val cappedB = min((b * maxColor).toInt, maxColor)
            s"$cappedR $cappedG $cappedB "
        } ++ Chunk.single("\n")

      def rowsStream(canvas: Canvas, maxColor: Int): ZStream[Any, Nothing, Chunk[String]] =
        for {
          rows     <- Stream.fromEffect(canvas.rows)
          rowArray <- Stream.fromIterable(rows)
        } yield rowToString(rowArray, maxColor)

      def channelSink(channel: AsynchronousFileChannel): Sink[Exception, Nothing, Chunk[Byte], Int] =
        Sink.foldLeftM(0) { (pos: Int, chunk: Chunk[Byte]) =>
          channel.write(chunk, pos).flatMap(written => UIO(pos + written))
        }

      override def render(canvas: Canvas, maxColor: Int): ZIO[Any, IOError, Unit] =
          AsynchronousFileChannel.open(zio.nio.file.Path.fromJava(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)
            .mapError(e => IOError.CanvasRenderingError("Error opening file", e)).provide(self).use{
            channel =>
              (for {
                _ <- (
                  Stream.fromEffect(headers(canvas, maxColor).map(Chunk(_))) ++
                    rowsStream(canvas, maxColor)
                  ).map(_.flatMap(_.getBytes |> Chunk.fromArray))
                  .run(channelSink(channel))
              } yield ()).mapError(e => IOError.CanvasRenderingError(e.getMessage, e))
          }
    }
  }

  object > extends CanvasSerializer.Service[CanvasSerializer] {
    override def render(canvas: Canvas, maxColor: Int): ZIO[CanvasSerializer, IOError, Unit] =
      ZIO.accessM(_.canvasSerializer.render(canvas, maxColor))
  }

}


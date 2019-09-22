package io.tuliplogic.raytracer.io.rendering

import java.nio.file.{Path, StandardOpenOption}

import io.tuliplogic.raytracer.errors.IOError
import io.tuliplogic.raytracer.model.{Canvas, Color}
import zio.nio.channels.AsynchronousFileChannel
import zio.stream._
import zio.{Chunk, UIO, ZIO}
import mouse.all._
import zio.blocking.Blocking

import scala.math.min

trait CanvasRenderer {
  def renderer: CanvasRenderer.Service[Any]
}

object CanvasRenderer {
  trait Service[R] {
    def render(canvas: Canvas, maxColor: Int): ZIO[R, IOError, Unit]
  }

  trait PPMCanvasRenderer extends CanvasRenderer with Blocking { self =>
    def path: Path
    override def renderer: Service[Any] = new Service[Any] {

      def formatHeader: String = "P3"
      def sizeHeader(c: Canvas): UIO[String] = c.width.zip(c.height).map{case (w, h) => s"$w $h"}
      def maxColorHeader(maxColor: Int): String = maxColor.toString

      def headers(c: Canvas, maxColor: Int): UIO[String] = for {
        sizeH <- sizeHeader(c)
      } yield         s"""$formatHeader
                         |$sizeH
                         |${maxColorHeader(maxColor)}
                         |""".stripMargin

      def rowToString(row: Array[Color], maxColor: Int): Chunk[String] = Chunk.fromArray(row).map {
        case Color(r, g, b) =>
          val cappedR = min((r * maxColor).toInt, maxColor)
          val cappedG = min((g * maxColor).toInt, maxColor)
          val cappedB = min((b * maxColor).toInt, maxColor)
          s"$cappedR $cappedG $cappedB "
      } ++ Chunk.single("\n")

      def rowsStream(canvas: Canvas, maxColor: Int): ZStream[Any, Nothing, Chunk[String]] = for {
        rows     <- Stream.fromEffect(canvas.rows)
        rowArray <- Stream.fromIterable(rows)
      } yield rowToString(rowArray, maxColor)

      def channelSink(channel: AsynchronousFileChannel): Sink[Exception, Nothing, Chunk[Byte], Int] =
        Sink.foldM(0) { (pos: Int, chunk: Chunk[Byte]) =>
          channel.write(chunk, pos).flatMap(written => UIO(ZSink.Step.more(pos + written)))
        }

      override def render(canvas: Canvas, maxColor: Int): ZIO[Any, IOError, Unit] =
        (for {
          channel <- AsynchronousFileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ).provide(self)
          _       <- (
            Stream.fromEffect(headers(canvas, maxColor).map(Chunk(_))) ++
            rowsStream(canvas, maxColor)
            )
//            .tap(s => UIO.effectTotal(print(s.toString() + "---------")))
            .map(_.flatMap(_.getBytes |> Chunk.fromArray))
           .run(channelSink(channel))
        } yield ()).mapError(e => IOError.CanvasRenderingError(e.getMessage, e))

    }
  }

}

object canvasIO extends CanvasRenderer.Service[CanvasRenderer] {
  override def render(canvas: Canvas, maxColor: Int): ZIO[CanvasRenderer, IOError, Unit] =
    ZIO.accessM(_.renderer.render(canvas, maxColor))
}

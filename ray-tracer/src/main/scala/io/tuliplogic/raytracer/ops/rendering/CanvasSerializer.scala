package io.tuliplogic.raytracer.ops.rendering

import java.nio.file.{Path, StandardOpenOption}

import com.sksamuel.scrimage.{Image, Pixel => SPixel}
import io.tuliplogic.raytracer.commons.errors.IOError
import io.tuliplogic.raytracer.ops.model.data.{Canvas, Color}
import zio.nio.channels.AsynchronousFileChannel
import zio.stream._
import zio.{Chunk, UIO, ZIO}
import mouse.all._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.console.Console

import scala.math.min

trait CanvasSerializer {
  val canvasSerializer: CanvasSerializer.Service[Any]
}

object CanvasSerializer {
  trait Service[R] {
    def serializeAsByteStream(canvas: Canvas, maxColor: Int): ZStreamChunk[R, Nothing, Byte]
    def serializeToFile(canvas: Canvas, maxColor: Int, path: Path): ZIO[R, IOError, Unit]
  }

  trait PPMCanvasSerializer extends CanvasSerializer with Blocking with Console with Clock { self =>
    val canvasSerializer: Service[Any] = new Service[Any] {

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

      def serializeAsByteStream(canvas: Canvas, maxColor: Int): ZStreamChunk[Any, Nothing, Byte] =
        ZStreamChunk(
          (
            Stream.fromEffect(headers(canvas, maxColor).map(Chunk(_))) ++
            rowsStream(canvas, maxColor)
          ).map(_.flatMap(_.getBytes |> Chunk.fromArray))
        )

      def serializeToFile(canvas: Canvas, maxColor: Int, path: Path): ZIO[Any, IOError, Unit] =
        AsynchronousFileChannel.open(zio.nio.file.Path.fromJava(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)
          .mapError(e => IOError.CanvasRenderingError("Error opening file", e)).use {
          channel =>
            serializeAsByteStream(canvas, maxColor).run(channelSink(channel)).unit
              .mapError(e => IOError.CanvasRenderingError(e.getMessage, e))
        }
    }
  }

  trait PNGCanvasSerializer extends CanvasSerializer {
    val maxColor = 255
    val canvasSerializer: Service[Any] = new Service[Any] {

      def serializeAsByteStream(canvas: Canvas, maxColor: Int): ZStreamChunk[Any, Nothing, Byte] = {
        val image = for {
          w <- canvas.width
          h <- canvas.height
          colors <- canvas.rows
        } yield Image(w, h, colors.flatten.map { c =>
              SPixel(
                min((c.red * maxColor).toInt, maxColor),
                min((c.green * maxColor).toInt, maxColor),
                min((c.blue * maxColor).toInt, maxColor), 255
              )
            })
        ZStream.fromEffect(image).chunkN(1).flatMap(
          img => ZStream.fromInputStream(img.stream).mapError(e => throw e)
        )
      }

      def serializeToFile(canvas: Canvas, maxColor: Int, path: Path): ZIO[Any, IOError, Unit] =
        AsynchronousFileChannel.open(zio.nio.file.Path.fromJava(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)
          .mapError(e => IOError.CanvasRenderingError("Error opening file", e)).use {
          channel =>
            serializeAsByteStream(canvas, maxColor).run(channelSink(channel)).unit
              .mapError(e => IOError.CanvasRenderingError(e.getMessage, e))
        }

      def channelSink(channel: AsynchronousFileChannel): Sink[Exception, Nothing, Chunk[Byte], Int] =
        Sink.foldLeftM(0) { (pos: Int, chunk: Chunk[Byte]) =>
          channel.write(chunk, pos).flatMap(written => UIO(pos + written))
        }

    }
  }

  object > extends CanvasSerializer.Service[CanvasSerializer] {
    override def serializeAsByteStream(canvas: Canvas, maxColor: Int): ZStreamChunk[CanvasSerializer, Nothing, Byte] =
      ZStream.fromEffect(ZIO.access[CanvasSerializer](cs => cs.canvasSerializer.serializeAsByteStream(canvas, maxColor))).chunkN(1).flatMap(identity)

    override def serializeToFile(canvas: Canvas, maxColor: Int, path: Path): ZIO[CanvasSerializer, IOError, Unit] =
      ZIO.accessM(_.canvasSerializer.serializeToFile(canvas, maxColor, path))
  }

}


package io.tuliplogic.raytracer.ops.rendering

import java.nio.file.Path

import com.sksamuel.scrimage.nio.PngWriter
import com.sksamuel.scrimage.{Image, ImageWriter, Pixel => SPixel}
import io.tuliplogic.raytracer.commons.errors.IOError
import io.tuliplogic.raytracer.ops.model.data.{Canvas, Color}
import zio.stream._
import zio.{Chunk, Has, IO, UIO, URLayer, ZIO, ZLayer}
import mouse.all._
import zio.blocking.Blocking
import zio.clock.Clock

import scala.math.min

object canvasSerializer {
  trait Service {
    def serializeAsByteStream(canvas: Canvas, maxColor: Int): Stream[Nothing, Byte]
    def serializeToFile(canvas: Canvas, maxColor: Int, path: Path): IO[IOError, Unit]
  }

  type CanvasSerializer = Has[Service]

  def serializeAsByteStream(canvas: Canvas, maxColor: Int): ZStream[CanvasSerializer, Nothing, Byte] =
    ZStream.fromEffect(ZIO.access[CanvasSerializer](_.get.serializeAsByteStream(canvas, maxColor))).chunkN(1).flatMap(identity)

  def serializeToFile(canvas: Canvas, maxColor: Int, path: Path): ZIO[CanvasSerializer, IOError, Unit] =
    ZIO.accessM(_.get.serializeToFile(canvas, maxColor, path))

  val ppmCanvasSerializer: ZLayer[Blocking with Clock, Nothing, CanvasSerializer] =
    ZLayer.fromFunction { _ =>
      new Service {
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

//          def channelSink(channel: AsynchronousFileChannel): Sink[Exception, Nothing, Chunk[Byte], Long] =
//            Sink.foldLeftM(0L) { (pos: Long, chunk: Chunk[Byte]) =>
//              channel.write(chunk, pos).flatMap(written => UIO(pos + written))
//            }

        def serializeAsByteStream(canvas: Canvas, maxColor: Int): Stream[Nothing, Byte] =
          (Stream.fromEffect(headers(canvas, maxColor).map(Chunk(_))) ++
            rowsStream(canvas, maxColor)).map(_.flatMap(_.getBytes |> Chunk.fromArray)).flattenChunks

        def serializeToFile(canvas: Canvas, maxColor: Int, path: Path): IO[IOError, Unit] = UIO(())
//            AsynchronousFileChannel.open(zio.nio.file.Path.fromJava(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)
//              .mapError(e => IOError.CanvasRenderingError("Error opening file", e)).use {
//              channel =>
//                serializeAsByteStream(canvas, maxColor).run(channelSink(channel)).unit
//                  .mapError(e => IOError.CanvasRenderingError(e.getMessage, e))
//            }
      }
    }

  val pNGCanvasSerializer: URLayer[Blocking, CanvasSerializer] = ZLayer.fromFunction(b =>
    new Service {

      implicit val imageWriter: ImageWriter = PngWriter.NoCompression

      def serializeAsByteStream(canvas: Canvas, maxColor: Int): Stream[Nothing, Byte] = {
        val image: UIO[Image] = for {
          w      <- canvas.width
          h      <- canvas.height
          colors <- canvas.rows
        } yield
          Image(
            w,
            h,
            colors.flatten.map { c =>
              SPixel(
                min((c.red * maxColor).toInt, maxColor),
                min((c.green * maxColor).toInt, maxColor),
                min((c.blue * maxColor).toInt, maxColor),
                255
              )
            }
          )
        ZStream
          .fromEffect(image)
          .chunkN(1)
          .flatMap(
            img => {
              println("getting image zio stream from image stream")
              println(s"img.size = ${img.width} x ${img.height}")
              val is = img.stream //making it strict to overcome bug in fromInputStream
              ZStream.fromInputStream(is).mapError(e => throw e).provide(b)
            }
          )
      }

      //TODO: reintroduce this when ZIO-nio is on RC18
      def serializeToFile(canvas: Canvas, maxColor: Int, path: Path): ZIO[Any, IOError, Unit] = UIO(())

      //        AsynchronousFileChannel.open(zio.nio.file.Path.fromJava(path), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.READ)
      //          .mapError(e => IOError.CanvasRenderingError("Error opening file", e)).use {
      //          channel =>
      //            serializeAsByteStream(canvas, maxColor).run(channelSink(channel)).unit
      //              .mapError(e => IOError.CanvasRenderingError(e.getMessage, e))
      //        }

      //      def channelSink(channel: AsynchronousFileChannel): Sink[Exception, Nothing, Chunk[Byte], Long] =
      //        Sink.foldLeftM(0L) { (pos: Long, chunk: Chunk[Byte]) =>
      //          channel.write(chunk, pos).flatMap(written => UIO(pos + written))
      //        }
  })
}

package io.tuliplogic.raytracer.io.rendering

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.model.{Canvas, Color}
import io.tuliplogic.geometry.matrix.AffineTransformations._
import io.tuliplogic.geometry.matrix.Matrix.Col
import io.tuliplogic.raytracer.errors.MatrixError
import org.scalatest.WordSpec
import zio.{DefaultRuntime, IO, console}
import zio.blocking.Blocking
import zio.stream.{Sink, Stream}


class CanvasRendererTest extends WordSpec with DefaultRuntime {

  val canvasFile = "/tmp/nioexp/canvas.ppm"
  val w = 5
  val h = 4

  "canvas renderer" should {
    "write canvas as PPM file" in {
      val cr  = new CanvasRenderer.PPMCanvasRenderer with Blocking.Live {
        override def path: Path = Paths.get(canvasFile)
      }
      unsafeRun {
        for {
          newCanvas <- Canvas.create(w, h)
          _ <- cr.renderer.render(newCanvas, 256)
        } yield ()
      }
    }

    "show that a point can rotate in the plane" in {
      val cr  = new CanvasRenderer.PPMCanvasRenderer with Blocking.Live {
        override def path: Path = Paths.get(canvasFile)
      }

      def updateCanvasFromXY(c: Canvas, p: Col): IO[MatrixError, Unit] = for {
        x <- p.get(0, 0)
        y <- p.get(1, 0)
        _ <- c.update(x.toInt, y.toInt, Color(255, 255, 255))
      }  yield ()

      import io.tuliplogic.geometry.matrix.MatrixOps.LiveMatrixOps.matrixOps
      val rotationAngle = math.Pi / 6
      val ww = 640
      val hh = 480
      unsafeRun{
        for {
          rotationMatrix   <- rotateZ(rotationAngle)
            scalingMatrix    <- scaling(ww / 2, hh / 2, 0)
            translationMtx   <- translation(ww / 2, hh / 2, 0)
            composed1        <- matrixOps.mul(rotationMatrix, scalingMatrix)
            composed2        <- matrixOps.mul(translationMtx, composed1)
            horizontalRadius <- vector(1, 0, 0)
            c                <- Canvas.create(ww, hh)
            positions        <- Stream.unfoldM(horizontalRadius)(v => matrixOps.mul(composed2, v).map(vv => Some((vv, vv))))
              .take(12).mapM {p => updateCanvasFromXY(c, p)}.run(Sink.collectAll)
//
//            _                <- console.putStrLn(positions.mkString("\n"))
//            _ <- cr.renderer.render(c, 256)
        } yield ()
      }
    }
  }


}

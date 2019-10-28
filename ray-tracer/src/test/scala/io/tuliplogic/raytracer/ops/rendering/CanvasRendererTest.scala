package io.tuliplogic.raytracer.ops.rendering

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec._
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.model.{Canvas, Color}
import org.scalatest.WordSpec
import zio.blocking.Blocking
import zio.stream.{Sink, ZStream}
import zio.{DefaultRuntime, IO, ZIO}

class CanvasRendererTest extends WordSpec with DefaultRuntime {

  val canvasFile = "/tmp/nioexp/canvas.ppm"
  val w          = 5
  val h          = 4

  val env = new ATModule.Live with MatrixModule.BreezeMatrixModule

  "canvas renderer" should {
    "write canvas as PPM file" in {
      val cr = new CanvasRenderer.PPMCanvasRenderer with Blocking.Live {
        override def path: Path = Paths.get(canvasFile)
      }
      unsafeRun {
        for {
          newCanvas <- Canvas.create(w, h)
          _         <- cr.renderer.render(newCanvas, 256)
        } yield ()
      }
    }

    "show that a point can rotate in the plane" in {
      val cr = new CanvasRenderer.PPMCanvasRenderer with Blocking.Live {
        override def path: Path = Paths.get(canvasFile)
      }

      def updateCanvasFromXY(c: Canvas, p: Pt): IO[AlgebraicError, Unit] =
        c.update(p.x.toInt, p.y.toInt, Color(255, 255, 255))

      val rotationAngle    = math.Pi / 12
      val ww               = 640
      val hh               = 480
      val horizontalRadius = Pt(1, 0, 0)

      unsafeRun {
        (for {
          rotateTf    <- ATModule.>.rotateZ(rotationAngle)
          scaleTf     <- ATModule.>.scale(math.min(ww, hh) / 3d, math.min(ww, hh) / 3d, 1d)
          translateTf <- ATModule.>.translate(ww / 2d, hh / 2d, 0d)
          composed    <- ATModule.>.compose(scaleTf, translateTf)
          c           <- Canvas.create(ww, hh)
          positions <- ZStream
            .unfoldM(horizontalRadius)(v => ATModule.>.applyTf(rotateTf, v).map(vv => Some((vv, vv))))
            .take(24)
            .mapM { p =>
              ATModule.>.applyTf(composed, p) flatMap { p =>
                updateCanvasFromXY(c, p)
              }
            }
            .run(Sink.collectAll[Unit])
          _ <- cr.renderer.render(c, 256)
        } yield ()).provide(env)
      }
    }
  }

}

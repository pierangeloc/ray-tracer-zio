package io.tuliplogic.raytracer.ops.rendering

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.ops.model.{Canvas, Color}
import io.tuliplogic.raytracer.geometry.affine.AffineTransformation._
import io.tuliplogic.raytracer.geometry.affine.{affineTfOps, AffineTransformationOps}
import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.PointVec._
import org.scalatest.WordSpec
import zio.{console, DefaultRuntime, IO}
import zio.blocking.Blocking
import zio.stream.{Sink, Stream, ZStream}

class CanvasRendererTest extends WordSpec with DefaultRuntime {

  val canvasFile = "/tmp/nioexp/canvas.ppm"
  val w          = 5
  val h          = 4

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
          rotateTf    <- rotateZ(rotationAngle)
          scaleTf     <- scale(math.min(ww, hh) / 3, math.min(ww, hh) / 3, 0)
          translateTf <- translate(ww / 2, hh / 2, 0)
          composed    <- scaleTf >=> translateTf
          c           <- Canvas.create(ww, hh)
          positions <- ZStream
            .unfoldM(horizontalRadius)(v => affineTfOps.transform(rotateTf, v).map(vv => Some((vv, vv))))
            .take(24)
            .mapM { p =>
              affineTfOps.transform(composed, p) flatMap { p =>
                updateCanvasFromXY(c, p)
              }
            }
            .run(Sink.collectAll)
          _ <- cr.renderer.render(c, 256)
        } yield ()).provide(AffineTransformationOps.BreezeMatrixOps$)
      }
    }
  }

}

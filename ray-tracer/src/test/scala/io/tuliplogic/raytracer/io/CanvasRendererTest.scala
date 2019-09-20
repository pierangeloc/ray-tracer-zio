package io.tuliplogic.raytracer.io

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.model.Canvas
import io.tuliplogic.raytracer.io
import org.scalatest.WordSpec
import zio.DefaultRuntime
import zio.blocking.Blocking

class CanvasRendererTest extends WordSpec with DefaultRuntime {

  val w = 5
  val h = 4

  "canvas renderer" should {
    "write canvas as PPM file" in {
      val cr  = new io.CanvasRenderer.PPMCanvasRenderer with Blocking.Live {
        override def path: Path = Paths.get("/tmp/nioexp/canvas.ppm")
      }
      unsafeRun {
        for {
          newCanvas <- Canvas.create(w, h)
          _ <- cr.renderer.render(newCanvas, 256)
        } yield ()
      }
    }
  }
}

package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.ops.model.data.{Canvas, Color}
import org.scalatest.Matchers._
import org.scalatest.WordSpec
import zio._

class CanvasTest extends WordSpec with DefaultRuntime {

  val w = 5
  val h = 4
  val pixels: Seq[(Int, Int)] = for {
    x <- 0 until w
    y <- 0 until h
  } yield (x, y)

  "canvas" should {
    "initialize as black upon creation" in {
      unsafeRun {
        for {
          newCanvas <- Canvas.create(w, h)
          colors    <- ZIO.traverse(pixels) { case (x, y) => newCanvas.get(x, y) }
          _         <- IO.effect(colors.forall(_ == Color.black) shouldEqual true)
        } yield ()
      }
    }

    "be updated correctly after creation" in {
      unsafeRun {
        for {
          newCanvas <- Canvas.create(w, h)
          _         <- newCanvas.update(1, 2, Color(255, 0, 0))
          color     <- newCanvas.get(1, 2)
          _         <- IO.effect(color shouldEqual Color(255, 0, 0))
        } yield ()
      }
    }

    "throw an error when accessing invalid elements" in {
      unsafeRun {
        (
          for {
            newCanvas <- Canvas.create(w, h)
            color     <- newCanvas.get(1, 6)
          } yield ()
        ).either
      }.isLeft shouldBe true
    }
  }
}

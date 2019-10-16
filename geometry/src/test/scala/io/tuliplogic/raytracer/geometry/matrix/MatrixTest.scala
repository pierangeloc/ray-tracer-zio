package io.tuliplogic.raytracer.geometry.matrix

import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.{Chunk, DefaultRuntime, IO, ZIO}

/**
  *
  * ray-tracer-zio - 21/09/2019
  * Created with ♥ in Amsterdam
  */
class MatrixTest extends WordSpec with DefaultRuntime {
  import Types._
  import vectorizable.comp

  "a matrix" should {
    "once created have its elements accessible by 0-based indexes" in {
      unsafeRun {
        for {
          m         <- factory.zero(2, 3)
          row1Elems <- ZIO.sequence(List(m.get(0, 0), m.get(0, 1), m.get(0, 2)))
          row2Elems <- ZIO.sequence(List(m.get(1, 0), m.get(1, 1), m.get(1, 2)))
          _         <- IO.effect { row1Elems.forall(_ == 0d) shouldEqual true }
          _         <- IO.effect { row2Elems.forall(_ == 0d) shouldEqual true }
        } yield ()
      }
    }

    "once created as column matrix has its elements accessible by 0-based indexes" in {
      unsafeRun {
        for {
          m         <- factory.fromRows(4, 1, comp(comp(0d), comp(0d), comp(0d), comp(0d)))
          col1Elems <- ZIO.sequence(List(m.get(0, 0), m.get(1, 0), m.get(2, 0), m.get(3, 0)))
          _         <- IO.effect { col1Elems.forall(_ == 0d) shouldEqual true }
        } yield ()
      }
    }

    "transpose successfully" in {
      unsafeRun {
        for {
          m          <- factory.fromRows(3, 2, comp(comp(0d, 1d), comp(2d, 3d), comp(4d, 5d)))
          transposed <- m.transpose
          elems <- ZIO.sequence(for {
            i <- 0 to 1
            j <- 0 to 2
          } yield transposed.get(i, j))
          _ <- IO.effect { elems shouldEqual List(0d, 2d, 4d, 1d, 3d, 5d) }
        } yield ()
      }
    }
  }
}

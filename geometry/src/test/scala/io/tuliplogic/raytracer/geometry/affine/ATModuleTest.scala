package io.tuliplogic.raytracer.geometry.affine

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule.MatrixTestService
import io.tuliplogic.raytracer.geometry.matrix.Types.{factory, _}
import io.tuliplogic.raytracer.geometry.matrix.{Matrix, MatrixModule}
import zio.test.Assertion._
import zio.test.{DefaultRunnableSpec, _}
import zio.{IO, Ref, UIO}


object ATModuleTest extends DefaultRunnableSpec(allSuites.s1) {

} 

object allSuites {
  import vectorizable.comp

  val translationMatrix: UIO[Matrix[L]] =
    factory
      .fromRows(
        4,
        4,
        comp(
          comp(1d, 0d, 0d, 3d),
          comp(0d, 1d, 0d, 4d),
          comp(0d, 0d, 1d, 5d),
          comp(0d, 0d, 0d, 1d)
        )
      ).orDie


  val invertedTranslationMatrix: IO[AlgebraicError, Matrix[L]] =
    factory
      .fromRows(
        4,
        4,
        comp(
          comp(1d, 0d, 0d, -3d),
          comp(0d, 1d, 0d, -4d),
          comp(0d, 0d, 1d, -5d),
          comp(0d, 0d, 0d, 1d)
        )
      ).orDie

  val ptVec = factory.createColVector(Vector(1d, 2d, 3d, 1d))
  val translatedPtVec: IO[AlgebraicError, factory.Col] = factory.createColVector(Vector(4, 6, 8, 1)).mapError(_.asInstanceOf[AlgebraicError])

  val s1 = suite("AT relies on matrix operations"){

    testM("Applying AT to a vector means invoking matrix multiplication on that vector") {
      val app = for {
        tf <- ATModule.>.translate(3d, 4d, 5d)
        pt <- ATModule.>.applyTf(tf, Pt(1d, 2d, 3d))
      } yield pt

      for {
        translMx <- translationMatrix
        ptColVec <- ptVec

        expectedCalls = Map(
          MatrixTestService.Op.Inv(translMx)           -> invertedTranslationMatrix,
          MatrixTestService.Op.Mul(translMx, ptColVec) -> translatedPtVec
        )
        mockState <- Ref.make(MatrixTestService.State(expectedCalls, Nil))
        matrixServiceMock <- UIO.succeed(MatrixTestService(mockState))
        pt <- app.provide(new ATModule.Live {
          override val matrixModule: MatrixModule.Service[Any] = matrixServiceMock
        })
        finalState <- mockState.get
      } yield assert(pt, equalTo(Pt(4d, 6d, 8d))) && assert(finalState.calls, equalTo(List(
        """Mul(| 1.0 0.0 0.0 3.0 |
          || 0.0 1.0 0.0 4.0 |
          || 0.0 0.0 1.0 5.0 |
          || 0.0 0.0 0.0 1.0 |,| 1.0 |
          || 2.0 |
          || 3.0 |
          || 1.0 |)""".stripMargin,
        """Inv(| 1.0 0.0 0.0 3.0 |
          || 0.0 1.0 0.0 4.0 |
          || 0.0 0.0 1.0 5.0 |
          || 0.0 0.0 0.0 1.0 |)""".stripMargin
        )))
    }
  }
}

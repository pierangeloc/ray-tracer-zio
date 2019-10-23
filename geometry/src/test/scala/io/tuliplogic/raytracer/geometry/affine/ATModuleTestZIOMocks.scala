package io.tuliplogic.raytracer.geometry.affine

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.geometry.matrix.Types._
import io.tuliplogic.raytracer.geometry.matrix.{Matrix, MatrixModule}
import zio.test.Assertion._
import zio.test.mock.{Method, Mock, Mockable}
import zio.test.mock.Expectation._
import zio.test.{DefaultRunnableSpec, _}
import zio.{IO, UIO, ZIO}


object ATModuleTestZIOMocks extends DefaultRunnableSpec(allSuites2.s1) {

}

trait MatrixModuleMock extends MatrixModule {
  val matrixModule: MatrixModuleMock.Service[Any]
}

object MatrixModuleMock {
  trait Service[R] extends MatrixModule.Service[R]

  object almostEqual extends Method[MatrixModuleMock, (M, M, Double), Boolean]
  object opposite extends Method[MatrixModuleMock, M, M] //def opposite(m: M): ZIO[R, AlgebraicError, M]
  object equal extends Method[MatrixModuleMock, (M, M), Boolean] //def equal(m1: M, m2: M): ZIO[R, AlgebraicError, Boolean]
  object add extends Method[MatrixModuleMock, (M, M), M]// def add(m1: M, m2: M): ZIO[R, AlgebraicError, M]
  object mul extends Method[MatrixModuleMock, (M, M), M] // def mul(m1: M, m2: M): ZIO[R, AlgebraicError, M]
  object had extends Method[MatrixModuleMock, (M, M), M]
  object invert extends Method[MatrixModuleMock, M, M]

  implicit val mockable: Mockable[MatrixModuleMock] = (mock: Mock) =>
    new MatrixModuleMock {
      override val matrixModule: Service[Any] = new Service[Any] {
        override def almostEqual(m1: M, m2: M, maxSquaredError: Double): ZIO[Any, AlgebraicError, Boolean] =
          mock(MatrixModuleMock.almostEqual, m1, m2, maxSquaredError)

        override def opposite(m: M): ZIO[Any, AlgebraicError, M] =
          mock(MatrixModuleMock.opposite, m)

        override def equal(m1: M, m2: M): ZIO[Any, AlgebraicError, Boolean] =
          mock(MatrixModuleMock.equal, m1, m2)

        override def add(m1: M, m2: M): ZIO[Any, AlgebraicError, M] =
          mock(MatrixModuleMock.add, m1, m2)

        override def mul(m1: M, m2: M): ZIO[Any, AlgebraicError, M] =
          mock(MatrixModuleMock.mul, m1, m2)

        override def had(m1: M, m2: M): ZIO[Any, AlgebraicError, M] =
          mock(MatrixModuleMock.had, m1, m2)

        override def invert(m: M): ZIO[Any, AlgebraicError, M] =
          mock(MatrixModuleMock.invert, m)
      }
    }
}


object allSuites2 {
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
      val app: ZIO[ATModule, AlgebraicError, Pt] = for {
        tf <- ATModule.>.translate(3d, 4d, 5d)
        pt <- ATModule.>.applyTf(tf, Pt(1d, 2d, 3d))
      } yield pt

      for {
        translMx <- translationMatrix
        invTranslMx <- invertedTranslationMatrix
        ptColVec <- ptVec
        translatedPtColVec <- translatedPtVec
        //set mocks
        mockedMatrix <- UIO {
          (MatrixModuleMock.invert(equalTo(translMx)) returns value(invTranslMx)) *>
          (MatrixModuleMock.mul(equalTo((translMx, ptColVec))) returns value(translatedPtColVec))
        }
        pt <- app.provideManaged(
                mockedMatrix.managedEnv.map { mm =>
                  new ATModule.Live {
                    override val matrixModule: MatrixModule.Service[Any] = mm.matrixModule
                  }
                }
              )
      } yield assert(pt, equalTo(Pt(4d, 6d, 8d)))
//      && assert(finalState.calls, equalTo(List(
//        """Mul(| 1.0 0.0 0.0 3.0 |
//          || 0.0 1.0 0.0 4.0 |
//          || 0.0 0.0 1.0 5.0 |
//          || 0.0 0.0 0.0 1.0 |,| 1.0 |
//          || 2.0 |
//          || 3.0 |
//          || 1.0 |)""".stripMargin,
//        """Inv(| 1.0 0.0 0.0 3.0 |
//          || 0.0 1.0 0.0 4.0 |
//          || 0.0 0.0 1.0 5.0 |
//          || 0.0 0.0 0.0 1.0 |)""".stripMargin
//        )))
    }
  }
}

package io.tuliplogic.raytracer.geometry.affine

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import zio.{UIO, URIO, ZIO}
import io.tuliplogic.raytracer.geometry.matrix.Types.{Col, M, factory, vectorizable}
//import zio.macros.access.accessible

import scala.math.{cos, sin}

case class AT(direct: M, inverse: M) {
  def inverted: AT = AT(inverse, direct)
}

trait ATModule {
  val aTModule: ATModule.Service[Any]
}

object ATModule {
  trait Service[R] {
    def applyTf(tf: AT, vec: Vec): ZIO[R, AlgebraicError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[R, AlgebraicError, Pt]
    def compose(first: AT, second: AT): ZIO[R, AlgebraicError, AT]
    def invert(tf: AT): ZIO[R, AlgebraicError, AT]
    def transpose(tf: AT): ZIO[R, AlgebraicError, AT]

    def translate(x: Double, y: Double, z: Double): ZIO[R, Nothing, AT]
    def scale(x: Double, y: Double, z: Double): ZIO[R, Nothing, AT]
    def rotateX(θ: Double): ZIO[R, Nothing, AT]
    def rotateY(θ: Double): ZIO[R, Nothing, AT]
    def rotateZ(θ: Double): ZIO[R, Nothing, AT]
    def shear(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): ZIO[R, Nothing, AT]
    def id: ZIO[R, Nothing, AT]
  }

  trait Live extends ATModule {
    val matrixModule: MatrixModule.Service[Any]

    val aTModule: ATModule.Service[Any] = new ATModule.Service[Any] {
      import vectorizable.comp

      private def transform(tf: AT, v: Col): ZIO[Any, AlgebraicError, Col] =
        for {
          v_m <- v.m
          v_n <- v.n
          _ <- if (v_m != 4 || v_n != 1)
            ZIO.fail(AlgebraicError.MatrixDimError(s"can't apply an affine transformation to a matrix $v_m x $v_n while expecting a matrix (vector) 4 x 1"))
          else ZIO.unit
          res <- matrixModule.mul(tf.direct, v)
        } yield res

      override def applyTf(tf: AT, vec: Vec): ZIO[Any, AlgebraicError, Vec] =
        for {
          col    <- PointVec.toCol(vec)
          colRes <- transform(tf, col)
          res    <- PointVec.colToVec(colRes)
        } yield res

      override def applyTf(tf: AT, pt: Pt): ZIO[Any, AlgebraicError, Pt] =
        for {
          col    <- PointVec.toCol(pt)
          colRes <- transform(tf, col)
          res    <- PointVec.colToPt(colRes)
        } yield res

      override def compose(first: AT, second: AT): ZIO[Any, AlgebraicError, AT] =
        for {
          direct  <- matrixModule.mul(second.direct, first.direct)
          inverse <- matrixModule.mul(first.inverse, second.inverse)
        } yield AT(direct, inverse)

      override def invert(tf: AT): ZIO[Any, AlgebraicError, AT] = UIO.succeed(AT(tf.inverse, tf.direct))

      override def transpose(tf: AT): ZIO[Any, AlgebraicError, AT] = for {
        direct <- tf.direct.transpose
        inverse <- matrixModule.invert(direct)
      } yield AT(direct, inverse)


      def fromDirect(direct: M): UIO[AT] =
        for {
          inverse <- matrixModule.invert(direct).orDie
        } yield AT(direct, inverse)

      override def translate(x: Double, y: Double, z: Double): URIO[Any, AT] =
        (
          for {
            direct <- factory
              .fromRows(
                4,
                4,
                comp(
                  comp(1d, 0d, 0d, x),
                  comp(0d, 1d, 0d, y),
                  comp(0d, 0d, 1d, z),
                  comp(0d, 0d, 0d, 1d)
                )
              )
            at <- fromDirect(direct)
          } yield at
        ).orDie

      override def scale(x: Double, y: Double, z: Double): URIO[Any, AT] =
        (
          for {
            direct <- factory
              .fromRows(
                4,
                4,
                comp(
                  comp(x, 0d, 0d, 0d),
                  comp(0d, y, 0d, 0d),
                  comp(0d, 0d, z, 0d),
                  comp(0d, 0d, 0d, 1d)
                )
              )
            at <- fromDirect(direct)
          } yield at
        ).orDie

      override def rotateX(θ: Double): URIO[Any, AT] =
        (
          for {
            direct <- factory
              .fromRows(
                4,
                4,
                comp(
                  comp(1d, 0d, 0d, 0d),
                  comp(0d, cos(θ), -sin(θ), 0d),
                  comp(0d, sin(θ), cos(θ), 0d),
                  comp(0d, 0d, 0d, 1d)
                ))
            at <- fromDirect(direct)
          } yield at
        ).orDie

      override def rotateY(θ: Double): URIO[Any, AT] =
        (
          for {
            direct <- factory
              .fromRows(
                4,
                4,
                comp(
                  comp(cos(θ), 0d, -sin(θ), 0d),
                  comp(0d, 1d, 0d, 0d),
                  comp(sin(θ), 0d, cos(θ), 0d),
                  comp(0d, 0d, 0d, 1d)
                ))
            at <- fromDirect(direct)
          } yield at
        ).orDie

      override def rotateZ(θ: Double): URIO[Any, AT] =
        (
          for {
            direct <- factory
              .fromRows(
                4,
                4,
                comp(
                  comp(cos(θ), -sin(θ), 0d, 0d),
                  comp(sin(θ), cos(θ), 0d, 0d),
                  comp(0d, 0d, 1d, 0d),
                  comp(0d, 0d, 0d, 1d)
                ))
            at <- fromDirect(direct)
          } yield at
        ).orDie

      override def shear(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): URIO[Any, AT] =
        (
          for {
            direct <- factory
              .fromRows(
                4,
                4,
                comp(
                  comp(1d, xY, xZ, 0d),
                  comp(yX, 1d, yZ, 0d),
                  comp(zX, zY, 1d, 0d),
                  comp(0d, 0d, 0d, 1d)
                ))
            at <- fromDirect(direct)
          } yield at
        ).orDie

      override def id: ZIO[Any, Nothing, AT] = translate(0, 0, 0)
    }

  }

  object > extends Service[ATModule] {
    def applyTf(tf: AT, vec: Vec): ZIO[ATModule, AlgebraicError, Vec] =
      ZIO.accessM(_.aTModule.applyTf(tf, vec))
    def applyTf(tf: AT, pt: Pt): ZIO[ATModule, AlgebraicError, Pt] =
      ZIO.accessM(_.aTModule.applyTf(tf, pt))
    def compose(first: AT, second: AT): ZIO[ATModule, AlgebraicError, AT] =
      ZIO.accessM(_.aTModule.compose(first, second))
    def invert(tf: AT): ZIO[ATModule, AlgebraicError, AT] =
      ZIO.accessM(_.aTModule.invert(tf))
    override def transpose(tf: AT): ZIO[ATModule, AlgebraicError, AT] =
      ZIO.accessM(_.aTModule.invert(tf))
    def translate(x: Double, y: Double, z: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.translate(x, y, z))
    def scale(x: Double, y: Double, z: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.scale(x, y, z))
    def rotateX(θ: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.rotateX(θ))
    def rotateY(θ: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.rotateY(θ))
    def rotateZ(θ: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.rotateZ(θ))
    def shear(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.shear(xY, xZ, yX, yZ, zX, zY))
    def id: ZIO[ATModule, Nothing, AT] = 
      ZIO.accessM(_.aTModule.id)
  }
}

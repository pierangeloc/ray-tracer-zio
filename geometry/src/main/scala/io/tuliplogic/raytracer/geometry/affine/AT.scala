package io.tuliplogic.raytracer.geometry.affine

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.matrix.{>, MatrixModule}
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import zio.{UIO, URIO, ZIO}
import io.tuliplogic.raytracer.geometry.matrix.Types.{factory, vectorizable, Col, M}
import scala.math.{cos, sin}

case class AT(direct: M, inverse: M)

trait ATModule {
  val service: ATModule.Service[Any]
}

object ATModule {
  trait Service[R] {
    def applyTf(tf: AT, vec: Vec): ZIO[R, AlgebraicError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[R, AlgebraicError, Pt]
    def compose(first: AT, second: AT): ZIO[R, AlgebraicError, AT]
    def invert(tf: AT): ZIO[R, AlgebraicError, AT]

    def translate(x: Double, y: Double, z: Double): URIO[R, AT]
    def scale(x: Double, y: Double, z: Double): URIO[R, AT]
    def rotateX(θ: Double): URIO[R, AT]
    def rotateY(θ: Double): URIO[R, AT]
    def rotateZ(θ: Double): URIO[R, AT]
    def shear(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): URIO[R, AT]
    def id: URIO[R, AT] = translate(0, 0, 0)
  }

  trait Live extends ATModule {
    val matrixService: MatrixModule.Service[Any]

    val service: ATModule.Service[Any] = new ATModule.Service[Any] {
      import vectorizable.comp

      private def transform(tf: AT, v: Col): ZIO[Any, AlgebraicError, Col] =
        for {
          v_m <- v.m
          v_n <- v.n
          _ <- if (v_m != 4 || v_n != 1)
            ZIO.fail(AlgebraicError.MatrixDimError(s"can't apply an affine transformation to a matrix $v_m x $v_n while expecting a matrix (vector) 4 x 1"))
          else ZIO.unit
          res <- matrixService.mul(tf.direct, v)
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
          direct  <- matrixService.mul(second.direct, first.direct)
          inverse <- matrixService.mul(first.inverse, second.inverse)
        } yield AT(direct, inverse)

      def fromDirect(direct: M): UIO[AT] =
        for {
          inverse <- matrixService.invert(direct).orDie
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

      override def invert(tf: AT): ZIO[Any, AlgebraicError, AT] = UIO.succeed(AT(tf.inverse, tf.direct))
    }

  }

  //TODO: use zio macros to generate the > object
}

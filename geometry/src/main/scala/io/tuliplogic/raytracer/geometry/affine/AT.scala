package io.tuliplogic.raytracer.geometry.affine

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import zio.{DefaultRuntime, UIO, ZIO}
import io.tuliplogic.raytracer.geometry.matrix.Types.{Col, M, factory, vectorizable}

import scala.math.{cos, sin}

//import zio.macros.access.accessible

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
    def invertible(
      x11:Double, x12:Double, x13:Double, x14:Double,
      x21:Double, x22:Double, x23:Double, x24:Double,
      x31:Double, x32:Double, x33:Double, x34:Double,
      x41:Double, x42:Double, x43:Double, x44:Double
    ): ZIO[R, AlgebraicError, AT]

    def translate(x: Double, y: Double, z: Double): ZIO[R, Nothing, AT] =
      invertible(
        1d, 0d, 0d, x,
        0d, 1d, 0d, y,
        0d, 0d, 1d, z,
        0d, 0d, 0d, 1d
      ).orDie

    def scale(x: Double, y: Double, z: Double): ZIO[R, Nothing, AT] =
      invertible(
        x, 0d, 0d, 0d,
        0d, y, 0d, 0d,
        0d, 0d, z, 0d,
        0d, 0d, 0d, 1d
      ).orDie

    def rotateX(θ: Double): ZIO[R, Nothing, AT] =
      invertible(
        1d, 0d, 0d, 0d,
        0d, cos(θ), -sin(θ), 0d,
        0d, sin(θ), cos(θ), 0d,
        0d, 0d, 0d, 1d
      ).orDie

    def rotateY(θ: Double): ZIO[R, Nothing, AT] =
      invertible(
        cos(θ), 0d, -sin(θ), 0d,
        0d, 1d, 0d, 0d,
        sin(θ), 0d, cos(θ), 0d,
        0d, 0d, 0d, 1d
      ).orDie

    def rotateZ(θ: Double): ZIO[R, Nothing, AT] =
      invertible(
        cos(θ), -sin(θ), 0d, 0d,
        sin(θ), cos(θ), 0d, 0d,
        0d, 0d, 1d, 0d,
        0d, 0d, 0d, 1d
      ).orDie

    def shear(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): ZIO[R, Nothing, AT] =
      invertible(
        1d, xY, xZ, 0d,
        yX, 1d, yZ, 0d,
        zX, zY, 1d, 0d,
        0d, 0d, 0d, 1d
      ).orDie

    def id: ZIO[R, Nothing, AT] = translate(0, 0, 0)
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
        } yield new AT(direct, inverse)

      override def invert(tf: AT): ZIO[Any, AlgebraicError, AT] = UIO.succeed(AT(tf.inverse, tf.direct))

      override def transpose(tf: AT): ZIO[Any, AlgebraicError, AT] = for {
        direct <- tf.direct.transpose
        inverse <- matrixModule.invert(direct)
      } yield AT(direct, inverse)

      override def invertible(
        x11:Double, x12:Double, x13:Double, x14:Double,
        x21:Double, x22:Double, x23:Double, x24:Double,
        x31:Double, x32:Double, x33:Double, x34:Double,
        x41:Double, x42:Double, x43:Double, x44:Double
      ): ZIO[Any, AlgebraicError, AT] = for {
        direct <- factory
          .fromRows(
            4,
            4,
            comp(
              comp(x11, x12, x13, x14),
              comp(x21, x22, x23, x24),
              comp(x31, x32, x33, x34),
              comp(x41, x42, x43, x44)
            )
          )
          inverse <- matrixModule.invert(direct).orDie
      } yield AT(direct, inverse)
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

    override def invertible(
      x11: Double, x12: Double, x13: Double, x14: Double,
      x21: Double, x22: Double, x23: Double, x24: Double,
      x31: Double, x32: Double, x33: Double, x34: Double,
      x41: Double, x42: Double, x43: Double, x44: Double): ZIO[ATModule, AlgebraicError, AT] =
      ZIO.accessM(_.aTModule.invertible(
        x11, x12, x13, x14,
        x21, x22, x23, x24,
        x31, x32, x33, x34,
        x41, x42, x43, x44
      ))
    override def transpose(tf: AT): ZIO[ATModule, AlgebraicError, AT] =
      ZIO.accessM(_.aTModule.invert(tf))
    override def translate(x: Double, y: Double, z: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.translate(x, y, z))
    override def scale(x: Double, y: Double, z: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.scale(x, y, z))
    override def rotateX(θ: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.rotateX(θ))
    override def rotateY(θ: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.rotateY(θ))
    override def rotateZ(θ: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.rotateZ(θ))
    override def shear(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.shear(xY, xZ, yX, yZ, zX, zY))

    override def id: ZIO[ATModule, Nothing, AT] =
      ZIO.accessM(_.aTModule.id)
  }
}

package io.tuliplogic.raytracer.geometry.affine

import io.tuliplogic.raytracer.commons.errors.ATError
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.matrix.Types.{Col, M, factory, vectorizable}
import io.tuliplogic.raytracer.geometry.matrix.matrixModule.MatrixModule
import zio.{Has, IO, UIO, ZIO, ZLayer}

import scala.math.{cos, sin}

case class AT(direct: M, inverse: M) {
  def inverted: AT = AT(inverse, direct)
}

object aTModule {

  type ATModule = Has[ATModule.Service]

  object ATModule {
    trait Service {
      def applyTf(tf: AT, vec: Vec): UIO[Vec]
      def applyTf(tf: AT, pt: Pt): UIO[Pt]
      def compose(first: AT, second: AT): UIO[AT]
      def invert(tf: AT): UIO[AT]
      def transpose(tf: AT): UIO[AT]
      def invertible(
        x11:Double, x12:Double, x13:Double, x14:Double,
        x21:Double, x22:Double, x23:Double, x24:Double,
        x31:Double, x32:Double, x33:Double, x34:Double,
        x41:Double, x42:Double, x43:Double, x44:Double
      ): IO[ATError, AT]

      def translate(x: Double, y: Double, z: Double): UIO[AT] =
        invertible(
          1d, 0d, 0d, x,
          0d, 1d, 0d, y,
          0d, 0d, 1d, z,
          0d, 0d, 0d, 1d
        ).orDie

      def scale(x: Double, y: Double, z: Double): UIO[AT] =
        invertible(
          x, 0d, 0d, 0d,
          0d, y, 0d, 0d,
          0d, 0d, z, 0d,
          0d, 0d, 0d, 1d
        ).orDie

      def rotateX(θ: Double): UIO[AT] =
        invertible(
          1d, 0d, 0d, 0d,
          0d, cos(θ), -sin(θ), 0d,
          0d, sin(θ), cos(θ), 0d,
          0d, 0d, 0d, 1d
        ).orDie

      def rotateY(θ: Double): UIO[AT] =
        invertible(
          cos(θ), 0d, -sin(θ), 0d,
          0d, 1d, 0d, 0d,
          sin(θ), 0d, cos(θ), 0d,
          0d, 0d, 0d, 1d
        ).orDie

      def rotateZ(θ: Double): UIO[AT] =
        invertible(
          cos(θ), -sin(θ), 0d, 0d,
          sin(θ), cos(θ), 0d, 0d,
          0d, 0d, 1d, 0d,
          0d, 0d, 0d, 1d
        ).orDie

      def shear(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): UIO[AT] =
        invertible(
          1d, xY, xZ, 0d,
          yX, 1d, yZ, 0d,
          zX, zY, 1d, 0d,
          0d, 0d, 0d, 1d
        ).orDie

      def id: UIO[AT] = translate(0, 0, 0)
    }
    val live: ZLayer[MatrixModule, Nothing, ATModule] = ZLayer.fromEnvironment { matrixModule =>

      Has(new Service {
        import vectorizable.comp

        private def transform(tf: AT, v: Col): ZIO[Any, ATError, Col] =
          for {
            v_m <- v.m
              v_n <- v.n
              _ <- if (v_m != 4 || v_n != 1)
                ZIO.fail(ATError(s"can't apply an affine transformation to a matrix $v_m x $v_n while expecting a matrix (vector) 4 x 1"))
              else ZIO.unit
              res <- matrixModule.get.mul(tf.direct, v).orDie
          } yield res

        override def applyTf(tf: AT, vec: Vec): ZIO[Any, Nothing, Vec] =
          for {
            col    <- PointVec.toCol(vec)
              colRes <- transform(tf, col).orDie
              res    <- PointVec.colToVec(colRes).orDie
          } yield res

        override def applyTf(tf: AT, pt: Pt): ZIO[Any, Nothing, Pt] =
          for {
            col    <- PointVec.toCol(pt)
              colRes <- transform(tf, col).orDie
              res    <- PointVec.colToPt(colRes).orDie
          } yield res

        override def compose(first: AT, second: AT): ZIO[Any, Nothing, AT] =
          for {
            direct  <- matrixModule.get.mul(second.direct, first.direct).orDie
              inverse <- matrixModule.get.mul(first.inverse, second.inverse).orDie
          } yield new AT(direct, inverse)

        override def invert(tf: AT): ZIO[Any, Nothing, AT] = UIO.succeed(AT(tf.inverse, tf.direct))

        override def transpose(tf: AT): ZIO[Any, Nothing, AT] = for {
          direct <- tf.direct.transpose
            inverse <- matrixModule.get.invert(direct).orDie
        } yield AT(direct, inverse)

        override def invertible(
          x11:Double, x12:Double, x13:Double, x14:Double,
          x21:Double, x22:Double, x23:Double, x24:Double,
          x31:Double, x32:Double, x33:Double, x34:Double,
          x41:Double, x42:Double, x43:Double, x44:Double
        ): ZIO[Any, ATError, AT] = for {
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
            ).mapError(e => ATError(e.toString))
            inverse <- matrixModule.get.invert(direct).mapError(e => ATError(e.toString))
        } yield AT(direct, inverse)
      })
    }

  }

  def applyTf(tf: AT, vec: Vec): ZIO[ATModule, Nothing, Vec] =
    ZIO.accessM(_.get.applyTf(tf, vec))
  def applyTf(tf: AT, pt: Pt): ZIO[ATModule, Nothing, Pt] =
    ZIO.accessM(_.get.applyTf(tf, pt))
  def compose(first: AT, second: AT): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.compose(first, second))
  def invert(tf: AT): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.invert(tf))

  def invertible(
    x11: Double, x12: Double, x13: Double, x14: Double,
    x21: Double, x22: Double, x23: Double, x24: Double,
    x31: Double, x32: Double, x33: Double, x34: Double,
    x41: Double, x42: Double, x43: Double, x44: Double): ZIO[ATModule, ATError, AT] =
    ZIO.accessM(_.get.invertible(
      x11, x12, x13, x14,
      x21, x22, x23, x24,
      x31, x32, x33, x34,
      x41, x42, x43, x44
    ))
  def transpose(tf: AT): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.invert(tf))
  def translate(x: Double, y: Double, z: Double): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.translate(x, y, z))
  def scale(x: Double, y: Double, z: Double): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.scale(x, y, z))
  def rotateX(θ: Double): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.rotateX(θ))
  def rotateY(θ: Double): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.rotateY(θ))
  def rotateZ(θ: Double): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.rotateZ(θ))
  def shear(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.shear(xY, xZ, yX, yZ, zX, zY))
  def id: ZIO[ATModule, Nothing, AT] =
    ZIO.accessM(_.get.id)
}

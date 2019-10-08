package io.tuliplogic.raytracer.geometry.vectorspace

import io.tuliplogic.raytracer.geometry.matrix.Types.{Col, M}
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec._
import io.tuliplogic.raytracer.geometry.matrix.{matrixOperations, MatrixOps, Types}
import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.matrix.{matrixOperations, MatrixOps}
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import zio.{UIO, ZIO}

//TODO: #1
// define affine transformations as a service that requires MatrixOps. applyAffineTransformations require an implementation of matrix ops
//TODO: #2
// to improve performance, store with every affine transformation, the inverse transformation to avoid performing too many inverse for nothing
/**
  * A transformation is by construction a 4 x 4 matrix. We need just to validate the vectors that it operates on are 4 x 1
  */
trait AffineTransformationOps {
  def affineTfOps: AffineTransformationOps.Service[Any]
}

object AffineTransformationOps {
  trait Service[R] {
    def transform(at: AffineTransformation, pt: Pt): ZIO[R, AlgebraicError, Pt]
    def transform(at: AffineTransformation, pt: Vec): ZIO[R, AlgebraicError, Vec]
    def compose(at1: AffineTransformation, at2: AffineTransformation): ZIO[R, AlgebraicError, AffineTransformation]
    def invert(at: AffineTransformation): ZIO[R, AlgebraicError, AffineTransformation]
    def transpose(at: AffineTransformation): ZIO[R, AlgebraicError, AffineTransformation]
  }

  trait Live extends AffineTransformationOps with MatrixOps { live =>
    override def affineTfOps: Service[Any] = new Service[Any] {

      private def transform(at: AffineTransformation, v: Col): ZIO[Any, AlgebraicError, Col] =
        for {
          v_m <- v.m
          v_n <- v.n
          _   <- if (v_m != 4 || v_n != 1) ZIO.fail(AlgebraicError.MatrixDimError(s"can't apply an affine transformation to a matrix $v_m x $v_n")) else ZIO.unit
          res <- matrixOperations.mul(at.m, v).provide(live)
        } yield res

      override def transform(at: AffineTransformation, pt: Pt): ZIO[Any, AlgebraicError, Pt] =
        for {
          col    <- toCol(pt)
          colRes <- transform(at, col)
          res    <- colToPt(colRes)
        } yield res

      override def transform(at: AffineTransformation, vec: Vec): ZIO[Any, AlgebraicError, Vec] =
        for {
          col    <- toCol(vec)
          colRes <- transform(at, col)
          res    <- colToVec(colRes)
        } yield res

      override def compose(at1: AffineTransformation, at2: AffineTransformation): ZIO[Any, AlgebraicError, AffineTransformation] =
        matrixOperations.mul(at2.m, at1.m).provide(live).map(AffineTransformation(_))

      override def invert(at: AffineTransformation): ZIO[Any, AlgebraicError, AffineTransformation] =
        matrixOperations.invert(at.m).map(AffineTransformation(_)).provide(live)

      override def transpose(at: AffineTransformation): ZIO[Any, AlgebraicError, AffineTransformation] =
        at.m.transpose.map(AffineTransformation(_))
    }
  }
  object Live extends Live with MatrixOps.Live
}

object affineTfOps extends AffineTransformationOps.Service[AffineTransformationOps] {
  override def transform(at: AffineTransformation, pt: Pt): ZIO[AffineTransformationOps, AlgebraicError, Pt] =
    ZIO.accessM(_.affineTfOps.transform(at, pt))

  override def transform(at: AffineTransformation, vec: Vec): ZIO[AffineTransformationOps, AlgebraicError, Vec] =
    ZIO.accessM(_.affineTfOps.transform(at, vec))

  override def compose(at1: AffineTransformation, at2: AffineTransformation): ZIO[AffineTransformationOps, AlgebraicError, AffineTransformation] =
    ZIO.accessM(_.affineTfOps.compose(at1, at2))

  override def invert(at: AffineTransformation): ZIO[AffineTransformationOps, AlgebraicError, AffineTransformation] =
    ZIO.accessM(_.affineTfOps.invert(at))

  override def transpose(at: AffineTransformation): ZIO[AffineTransformationOps, AlgebraicError, AffineTransformation] =
    ZIO.accessM(_.affineTfOps.transpose(at))
}

//TODO: see if we can define an affinetransformation as a function Pt => Pt | Vec => Vec
// def f[A <: SpatialEntity](a: A): A
case class AffineTransformation(m: M) {
  def >=>(next: AffineTransformation): ZIO[AffineTransformationOps, Nothing, AffineTransformation] =
    affineTfOps.compose(this, next).orDie
}

object AffineTransformation {
  import Types._
  import vectorizable.comp

  import math.{cos, sin}
  def composeLeft(tf: AffineTransformation*): ZIO[AffineTransformationOps, Nothing, AffineTransformation] =
    tf.toList.foldLeft(id.provideSome((x: AffineTransformationOps) => x)) {
      case (accF, next) =>
        for {
          acc <- accF
          res <- acc >=> next
        } yield res
    }

  def id: UIO[AffineTransformation] = translate(0, 0, 0)

  def translate(x: Double, y: Double, z: Double): UIO[AffineTransformation] =
    factory
      .fromRows(
        4,
        4,
        comp(
          comp(1d, 0d, 0d, x),
          comp(0d, 1d, 0d, y),
          comp(0d, 0d, 1d, z),
          comp(0d, 0d, 0d, 1d)
        ))
      .map(new AffineTransformation(_))
      .orDie

  def scale(x: Double, y: Double, z: Double): UIO[AffineTransformation] =
    factory
      .fromRows(
        4,
        4,
        comp(
          comp(x, 0d, 0d, 0d),
          comp(0d, y, 0d, 0d),
          comp(0d, 0d, z, 0d),
          comp(0d, 0d, 0d, 1d)
        ))
      .map(new AffineTransformation(_))
      .orDie

  def rotateX(θ: Double): UIO[AffineTransformation] =
    factory
      .fromRows(
        4,
        4,
        comp(
          comp(1d, 0d, 0d, 0d),
          comp(0d, cos(θ), -sin(θ), 0d),
          comp(0d, sin(θ), cos(θ), 0d),
          comp(0d, 0d, 0d, 1d)
        ))
      .map(new AffineTransformation(_))
      .orDie

  def rotateY(θ: Double): UIO[AffineTransformation] =
    factory
      .fromRows(
        4,
        4,
        comp(
          comp(cos(θ), 0d, -sin(θ), 0d),
          comp(0d, 1d, 0d, 0d),
          comp(sin(θ), 0d, cos(θ), 0d),
          comp(0d, 0d, 0d, 1d)
        ))
      .map(new AffineTransformation(_))
      .orDie

  def rotateZ(θ: Double): UIO[AffineTransformation] =
    factory
      .fromRows(
        4,
        4,
        comp(
          comp(cos(θ), -sin(θ), 0d, 0d),
          comp(sin(θ), cos(θ), 0d, 0d),
          comp(0d, 0d, 1d, 0d),
          comp(0d, 0d, 0d, 1d)
        ))
      .map(new AffineTransformation(_))
      .orDie

  def shearing(xY: Double, xZ: Double, yX: Double, yZ: Double, zX: Double, zY: Double): UIO[AffineTransformation] =
    factory
      .fromRows(
        4,
        4,
        comp(
          comp(1d, xY, xZ, 0d),
          comp(yX, 1d, yZ, 0d),
          comp(zX, zY, 1d, 0d),
          comp(0d, 0d, 0d, 1d)
        ))
      .map(new AffineTransformation(_))
      .orDie

}

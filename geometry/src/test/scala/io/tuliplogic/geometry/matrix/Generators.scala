package io.tuliplogic.geometry.matrix

import org.scalacheck.Gen
import zio.{Chunk, DefaultRuntime}

trait Generators { self: DefaultRuntime =>

  def matrixGenWithDim(m: Int, n: Int): Gen[Matrix] =
    for {
      elems  <- Gen.listOfN(m * n, Gen.chooseNum[Double](-1000, 1000))
      matrix <- Gen.const(unsafeRun(Matrix.fromRows(m, n, Chunk.fromIterable(elems.grouped(n).map(Chunk.fromIterable).toIterable))))
    } yield matrix

  def matrixGen: Gen[Matrix] =
    for {
      m      <- Gen.chooseNum(1, 100)
      n      <- Gen.chooseNum(1, 100)
      matrix <- matrixGenWithDim(m, n)
    } yield matrix

  def vectorGen: Gen[Matrix] = for {
    elems  <- Gen.listOfN(3, Gen.chooseNum[Double](-1000, 1000))
    vector <- Gen.const(unsafeRun(AffineTransformations.vector(elems(0), elems(1), elems(2))))
  } yield vector

  def pointGen: Gen[Matrix] = for {
    elems  <- Gen.listOfN(3, Gen.chooseNum[Double](-1000, 1000))
    point <- Gen.const(unsafeRun(AffineTransformations.point(elems(0), elems(1), elems(2))))
  } yield point

  val reasonableDouble = Gen.chooseNum[Double](-1000, 1000)

}

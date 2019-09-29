package io.tuliplogic.raytracer.geometry.matrix

import io.tuliplogic.raytracer.geometry.vectorspace.PointVec._
import org.scalacheck.Gen
import zio.{Chunk, DefaultRuntime}

trait Generators { self: DefaultRuntime =>

  import Types._
  def matrixGenWithDim(m: Int, n: Int): Gen[M] =
    for {
      elems  <- Gen.listOfN(m * n, Gen.chooseNum[Double](-1000, 1000))
      matrix <- Gen.const(unsafeRun(factory.fromRows(m, n, vectorizable.fromArray(elems.grouped(n).map(xs => vectorizable.fromArray(xs.toArray)).toArray))))
    } yield matrix

  def matrixGen: Gen[M] =
    for {
      m      <- Gen.chooseNum(1, 100)
      n      <- Gen.chooseNum(1, 100)
      matrix <- matrixGenWithDim(m, n)
    } yield matrix

  def vectorGen: Gen[Vec] =
    for {
      elems  <- Gen.listOfN(3, Gen.chooseNum[Double](-1000, 1000))
      vector <- Gen.const(Vec(elems(0), elems(1), elems(2)))
    } yield vector

  def pointGen: Gen[Pt] =
    for {
      elems <- Gen.listOfN(3, Gen.chooseNum[Double](-1000, 1000))
      point <- Gen.const(Pt(elems(0), elems(1), elems(2)))
    } yield point

  val reasonableDouble = Gen.chooseNum[Double](-1000, 1000)

}

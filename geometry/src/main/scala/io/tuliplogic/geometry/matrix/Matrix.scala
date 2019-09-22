package io.tuliplogic.geometry.matrix

import cats.Functor
import cats.implicits._
import io.tuliplogic.raytracer.errors.MatrixError.{IndexExceedMatrixDimension, MatrixConstructionError}
import mouse.all._
import zio.{IO, _}

import scala.reflect.ClassTag

//matrix m rows by n cols
class Matrix[L[_]] private (private val m_ : Int, n_ : Int, rows_ : L[L[Double]])(implicit L: Vectorizable[L], ct: ClassTag[L[Double]]) {
  private val cols_ : L[L[Double]] = L.toArray(rows_).map(L.toArray).transpose.map(L.fromArray).toArray |> L.fromArray[L[Double]]
//  private val cols_ : Chunk[Chunk[Double]] = rows_.toArray.map(_.toArray).transpose.map(Chunk.fromArray) |> Chunk.fromArray
  def m: UIO[Int]                     = UIO.succeed(m_)
  def n: UIO[Int]                     = UIO.succeed(n_)
  def rows: UIO[L[L[Double]]] = UIO.succeed(rows_)
  def cols: UIO[L[L[Double]]] = UIO.succeed(cols_)
  def transpose: UIO[Matrix[L]]          = UIO.succeed(new Matrix(n_, m_, cols_))
  def get(i: Int, j: Int): IO[IndexExceedMatrixDimension, Double] =
    Matrix.checkAccessIndex(i, j, m_, n_) *>
//    UIO.effectTotal(5.0)
      UIO.effectTotal {
        val res = L.get(L.get(rows_)(i))(j)
        res
      }

  override def toString: String = L.toArray(rows_).map(rs => "| " +  L.toArray(rs).mkString(" ") + " |").mkString("\n")
}

object Matrix {

  private def checkAccessIndex(x: Int, y: Int, rows: Int, cols: Int): IO[IndexExceedMatrixDimension, Unit] =
    if (x >= 0 && x < rows && y >= 0 && y < cols) IO.unit
    else IO.fail(IndexExceedMatrixDimension(x, y, rows, cols))

  class MatrixFactory[L[_]](implicit L: Vectorizable[L], F: Functor[L], CT: ClassTag[L[Double]]) {
    type Row = Matrix[L]
    type Col = Matrix[L]

    def hom(m: Int, n: Int, value: Double): UIO[Matrix[L]] = UIO {
      val elems = L.fromArray(Array.fill(m)(L.fromArray(Array.fill(n)(value))))
      new Matrix[L](m, n, elems)
    }

    def zero(m: Int, n: Int): UIO[Matrix[L]] = hom(m, n, 0)
    def ones(m: Int, n: Int): UIO[Matrix[L]] = hom(m, n, 1)
    def eye(n: Int): UIO[Matrix[L]] = fromRows(n, n,
      L.fromArray(Array.tabulate(n,n)((x,y) => if(x==y) 1d else 0d).map(L.fromArray))
    ).orDie

    def createRowVector(elems: L[Double]): UIO[Row] = UIO.succeed(new Matrix[L](1, L.length(elems), L.fromArray(Array(elems))))
    def createColVector(elems: L[Double]): UIO[Col] = UIO.succeed(new Matrix[L](L.length(elems), 1, elems.map(x => L.fromArray(Array(x)))))

    def fromRows(m: Int, n: Int, rows: L[L[Double]]): ZIO[Any, MatrixConstructionError, Matrix[L]] =
      for {
        _ <- if (L.length(rows) == m && L.toArray(rows).forall(r => L.length(r) == n)) IO.unit
        else IO.fail(MatrixConstructionError(s"can't build a matrix out of these rows as dimensions are not correct: m = $m, n = $n, rows.length = ${L.length(rows)}"))
      } yield
        //trying to fix a boxing issue with Chunk //TODO: find a minimal example that shows this weird chunk behavior
        new Matrix(m, n, rows.map(chunk => L.fromArray(L.toArray(chunk)))) {}
  }

}

package io.tuliplogic.geometry.matrix

import zio.{Chunk => ZioChunk}
import fs2.{Chunk => Fs2Chunk}

import scala.reflect.ClassTag

trait Vectorizable[L[_]] {
  def fromArray[A](as: Array[A]): L[A]

  def toArray[A: ClassTag](as: L[A]): Array[A]
  def length[A](as: L[A]): Int
  def get[A](as: L[A])(i: Int): A
  def zip[A, B, C](as: L[A], bs: L[B])(f: (A, B) => C): L[C]

  def comp[A: ClassTag](as: A*): L[A]                       = fromArray(as.toArray)
  def scalarProduct(row: L[Double], col: L[Double]): Double = toArray(zip(row, col)(_ * _)).foldLeft(0d)(_ + _)
  def l2(row: L[Double]): Double                            = scalarProduct(row, row)
  def groupChunk[A: ClassTag](chunk: L[A])(groupSize: Int)(implicit CT: ClassTag[L[A]]): L[L[A]] =
    fromArray(toArray(chunk).grouped(groupSize).map(fromArray).toArray)
}

object Vectorizable {

  def apply[L[_]](implicit ev: Vectorizable[L]): Vectorizable[L] = ev

  implicit val zioChunkVectorizable: Vectorizable[ZioChunk] = new Vectorizable[ZioChunk] {
    override def fromArray[A](as: Array[A]): ZioChunk[A]                                     = ZioChunk.fromArray(as)
    override def toArray[A: ClassTag](as: ZioChunk[A]): Array[A]                             = as.toArray
    override def length[A](as: ZioChunk[A]): Int                                             = as.length
    def get[A](as: ZioChunk[A])(i: Int): A                                                   = as.toArray(i)
    override def zip[A, B, C](as: ZioChunk[A], bs: ZioChunk[B])(f: (A, B) => C): ZioChunk[C] = as.zipWith(bs)(f)
  }

  implicit val vectorVectorizable: Vectorizable[Vector] = new Vectorizable[Vector] {
    override def fromArray[A](as: Array[A]): Vector[A]                                 = as.toVector
    override def toArray[A: ClassTag](as: Vector[A]): Array[A]                         = as.toArray
    override def length[A](as: Vector[A]): Int                                         = as.length
    override def get[A](as: Vector[A])(i: Int): A                                      = as(i)
    override def zip[A, B, C](as: Vector[A], bs: Vector[B])(f: (A, B) => C): Vector[C] = as.zip(bs).map(f.tupled)
  }

  implicit val fs2ChunkVectorizable: Vectorizable[Fs2Chunk] = new Vectorizable[Fs2Chunk] {
    override def fromArray[A](as: Array[A]): Fs2Chunk[A]                                     = Fs2Chunk.array(as)
    override def toArray[A: ClassTag](as: Fs2Chunk[A]): Array[A]                             = as.toArray
    override def length[A](as: Fs2Chunk[A]): Int                                             = as.size
    override def get[A](as: Fs2Chunk[A])(i: Int): A                                          = as(i)
    override def zip[A, B, C](as: Fs2Chunk[A], bs: Fs2Chunk[B])(f: (A, B) => C): Fs2Chunk[C] = as.zipWith(bs)(f)
  }

}

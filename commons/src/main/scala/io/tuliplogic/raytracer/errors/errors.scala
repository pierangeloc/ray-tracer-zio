package io.tuliplogic.raytracer.errors


sealed trait RayTracerError extends Exception


/**
 * Errors for matrix handling
 */
sealed trait MatrixError extends RayTracerError
object MatrixError {
  case class MatrixDimError(str: String) extends MatrixError
  case object MatrixNotInvertible extends MatrixError

  case class IndexExceedMatrixDimension(row: Int, col: Int, m: Int, n: Int) extends MatrixError {
    override def getMessage = s"Attempted to access index ($row, $col) in a matrix $m x $n"
  }

  case class MatrixConstructionError(override val getMessage: String) extends MatrixError
}

/**
 * Errors for Canvas handling
 */
sealed trait CanvasError extends RayTracerError
object CanvasError {
  case class IndexExceedCanvasDimension(x: Int, y: Int, width: Int, height: Int) extends MatrixError {
    override def getMessage = s"Attempted to access index ($x, $y) in a canvas $width x $height"
  }
}

sealed trait IOError extends RayTracerError
object IOError {
  case class CanvasRenderingError(override val getMessage: String, override val getCause: Throwable) extends IOError
}

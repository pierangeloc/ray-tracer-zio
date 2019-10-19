package io.tuliplogic.raytracer.geometry.vectorspace

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import zio.{IO, ZIO}
import io.tuliplogic.raytracer.geometry.matrix.Types.M


sealed trait AT {
  val backing: M
}


trait ATModule {
  val service: ATModule.Service[Any]
}

object ATModule {
  trait Service[R] {
    def applyTf(tf: AT, vec: Vec): ZIO[R, AlgebraicError, Vec]
    def applyTf(tf: AT, pt: Pt): ZIO[R, AlgebraicError, Pt]
    def compose(first: AT, second: AT): ZIO[R, AlgebraicError, AT]
  }
}

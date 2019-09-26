package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.SpatialEntity.{Pt, Vec}
import io.tuliplogic.geometry.matrix.SpatialEntity.SceneObject.Sphere
import zio.ZIO

trait SpatialEntityOperations {
  def spatEntityOps: SpatialEntityOperations.Service[Any]
}

object SpatialEntityOperations {
  trait Service[R] {
    def normal(p: Pt, s: Sphere): ZIO[R, Nothing, Vec]
  }
}
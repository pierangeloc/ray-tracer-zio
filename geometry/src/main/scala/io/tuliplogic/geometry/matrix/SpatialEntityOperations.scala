package io.tuliplogic.geometry.matrix

import io.tuliplogic.geometry.matrix.SpatialEntity.{Pt, Vec}
import io.tuliplogic.geometry.matrix.SpatialEntity.SceneObject.Sphere
import zio.{UIO, ZIO}

trait SpatialEntityOperations {
  def spatEntityOperations: SpatialEntityOperations.Service[Any]
}

object SpatialEntityOperations {
  trait Service[R] {
    def normal(p: Pt, s: Sphere): ZIO[R, Nothing, Vec]
  }

  trait Live extends SpatialEntityOperations with AffineTransformationOps {
    def spatEntityOperations: Service[Any] = new Service[Any] {
      def normal(p: Pt, s: Sphere): ZIO[Any, Nothing, Vec] =
        (for {
          inverseTf <- affineTfOps.invert(s.transformation)
          objectPt  <- affineTfOps.transform(inverseTf, p)
          objectNormal <- UIO.succeed(objectPt - Pt(0, 0, 0))
          inverseTransposed <- affineTfOps.transpose(inverseTf)
          worldNormal  <- affineTfOps.transform(inverseTransposed, objectNormal)
          normalized   <- worldNormal.normalized
        } yield normalized).orDie
    }
  }

  object Live extends Live with AffineTransformationOps.Live with MatrixOps.Live
}

object spatialEntityOps extends SpatialEntityOperations.Service[SpatialEntityOperations]{
  override def normal(p: Pt, s: Sphere): ZIO[SpatialEntityOperations, Nothing, Vec] =
    ZIO.accessM(_.spatEntityOperations.normal(p, s))
}
package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.matrix.MatrixOps
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformationOps
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.Sphere
import zio.{UIO, ZIO}

trait SpatialEntityOperations {
  def spatEntityOperations: SpatialEntityOperations.Service[Any]
}

//TODO: test PBT that the angle between reflect and normal, and incident and normal is equal. Also, the 2 vectors should be coplanar
object SpatialEntityOperations {
  trait Service[R] {
    def normal(p: Pt, s: Sphere): ZIO[R, Nothing, Vec]
    def reflect(vec: Vec, normal: Vec): ZIO[R, Nothing, Vec] =
      ZIO.succeed(vec.minus(normal.scale(-2 * vec.dot(normal))))
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
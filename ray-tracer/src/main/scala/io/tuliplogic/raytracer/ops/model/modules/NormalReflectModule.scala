package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.data.Scene.{Plane, Shape, Sphere}
import zio.{UIO, ZIO}

trait NormalReflectModule {
  val normalReflectModule: NormalReflectModule.Service[Any]
}

//TODO: test PBT that the angle between reflect and normal, and incident and normal is equal. Also, the 2 vectors should be coplanar
//TODO: make these operations just shape related operations, because a shape is able to calculate the normal to itself at a given point, and calculate the reflected vector
object NormalReflectModule {

  trait Service[R] {
    def normal(p: Pt, o: Shape): ZIO[R, Nothing, Vec]
    final def reflect(vec: Vec, normal: Vec): ZIO[R, Nothing, Vec] =
      ZIO.succeed(vec.-(normal.*(2 * vec.dot(normal))))
  }

  trait Live extends NormalReflectModule {

    val aTModule: ATModule.Service[Any]

    val normalReflectModule: Service[Any] = new Service[Any] {

      def canonicalNormal(p: Pt, o: Shape): UIO[Vec] = o match {
        case Sphere(_, _) => UIO.succeed(p - Pt(0, 0, 0))
        case Plane(_, _)  => UIO.succeed(Vec(0, 1, 0))
      }

      def normal(p: Pt, o: Shape): ZIO[Any, Nothing, Vec] =
        (for {
          inverseTf         <- aTModule.invert(o.transformation)
          objectPt          <- aTModule.applyTf(inverseTf, p)
          objectNormal      <- canonicalNormal(objectPt, o)
          inverseTransposed <- aTModule.transpose(inverseTf)
          worldNormal       <- aTModule.applyTf(inverseTransposed, objectNormal)
          normalized        <- worldNormal.normalized
        } yield normalized).orDie
    }
  }

}

object spatialEntityOps extends NormalReflectModule.Service[NormalReflectModule] {
  override def normal(p: Pt, o: Shape): ZIO[NormalReflectModule, Nothing, Vec] =
    ZIO.accessM(_.normalReflectModule.normal(p, o))
}

package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.ops.model.data.Scene.{Plane, Shape, Sphere}
import zio.{Has, UIO, ZIO, ZLayer}


//TODO: test PBT that the angle between reflect and normal, and incident and normal is equal. Also, the 2 vectors should be coplanar
//TODO: make these operations just shape related operations, because a shape is able to calculate the normal to itself at a given point, and calculate the reflected vector
object normalReflectModule {

  trait Service {
    def normal(p: Pt, o: Shape): UIO[Vec]

    final def reflect(vec: Vec, normal: Vec): UIO[Vec] =
      (vec - (normal * (2 * (vec dot normal)))).normalized.orDie
  }

  type NormalReflectModule = Has[Service]

  val live: ZLayer[ATModule, Nothing, NormalReflectModule] = ZLayer.fromService { aTModule =>
    new Service {

      def canonicalNormal(p: Pt, o: Shape): UIO[Vec] = o match {
        case Sphere(_, _) => UIO.succeed(p - Pt(0, 0, 0))
        case Plane(_, _) => UIO.succeed(Vec(0, 1, 0))
      }

      def normal(p: Pt, o: Shape): ZIO[Any, Nothing, Vec] =
        (for {
          inverseTf <- aTModule.invert(o.transformation)
          objectPt <- aTModule.applyTf(inverseTf, p)
          objectNormal <- canonicalNormal(objectPt, o)
          inverseTransposed <- aTModule.transpose(inverseTf)
          worldNormal <- aTModule.applyTf(inverseTransposed, objectNormal)
          normalized <- worldNormal.normalized
        } yield normalized).orDie
    }
  }

  def normal(p: Pt, o: Shape): ZIO[NormalReflectModule, Nothing, Vec] =
    ZIO.accessM(_.get.normal(p, o))


}

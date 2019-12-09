package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.commons.errors.BusinessError.GenericError
import io.tuliplogic.raytracer.ops.model.data.Scene.Shape
import io.tuliplogic.raytracer.ops.model.data.{Intersection, Ray}
import io.tuliplogic.raytracer.ops.model.modules.PhongReflectionModule.HitComps
import zio.{UIO, ZIO}


trait WorldHitCompsModule {
  val worldHitCompsModule: WorldHitCompsModule.Service[Any]
}

object WorldHitCompsModule {
  trait Service[R] {
    /**
      * Determines the hit components for an intersection, among the list of intersections
      * This is important to handle transparency, as the first intersection is not sufficient, and we must handle also subsequent ones to determine
      * the refraction indexes between materials
      */
    def hitComps(ray: Ray, hit: Intersection, intersections: List[Intersection]): ZIO[R, GenericError, HitComps]
  }

  trait Live extends WorldHitCompsModule {
    val normalReflectModule: NormalReflectModule.Service[Any]

    val worldHitCompsModule: WorldHitCompsModule.Service[Any] = new Service[Any] {

      override def hitComps(ray: Ray, hit: Intersection, intersections: List[Intersection]): ZIO[Any, GenericError, HitComps] = {
        type Z = (List[Shape], Option[Double], Option[Double])

        /**
          * We can calculate the n1, n2 for the hit, given the list of intersections. Each intersection carries the object, together with its material
          *
          * @return
          */
        def n1n2: UIO[(Double, Double)] = {

          val maybeN1N2: (List[Shape], Option[Double], Option[Double]) = intersections.foldLeft[Z]((Nil, None, None)) {
            case (in@(xs, Some(n1), Some(n2)), _) =>
              in
            case ((conts, None, None), `hit`) =>
              val n1: Double = conts.lastOption.map(_.material.refractionIndex).getOrElse(1)
              val contss =
                if (conts.contains(hit.sceneObject))
                  conts.filter(_ != hit.sceneObject)
                else
                  conts :+ hit.sceneObject
              val n2: Double = contss.lastOption.map(_.material.refractionIndex).getOrElse(1)
              (contss, Some(n1), Some(n2))

            case ((conts, None, None), i) =>
              val contss = if (conts.contains(i.sceneObject)) conts.filter(_ != i.sceneObject)
              else conts :+ i.sceneObject
              (contss, None, None)

            case _ => (Nil, None, None)
          }

          maybeN1N2 match {
            case (_, Some(n1), Some(n2)) => UIO.succeed((n1, n2))
            case _ => ZIO.die(new IllegalArgumentException("can't determine refraction indexes")) //TODO: proper error mgmgt
          }

        }

        for {
          pt <- UIO(ray.positionAt(hit.t))
          normalV <- normalReflectModule.normal(pt, hit.sceneObject)
          eyeV <- ray.direction.normalized.map(- _).orDie
          realNormal <- UIO(if ((eyeV dot normalV) > 0) normalV else -normalV)
          reflectV <- normalReflectModule.reflect(ray.direction, realNormal)
          (n1, n2) <- n1n2
        } yield HitComps(hit.sceneObject, pt, realNormal, eyeV, reflectV, n1, n2)
      }
    }
  }

  object > extends WorldHitCompsModule.Service[WorldHitCompsModule] {
   override def hitComps(ray: Ray, hit: Intersection, intersections: List[Intersection]): ZIO[WorldHitCompsModule, GenericError, HitComps] =
     ZIO.accessM(_.worldHitCompsModule.hitComps(ray, hit, intersections))
  }

}

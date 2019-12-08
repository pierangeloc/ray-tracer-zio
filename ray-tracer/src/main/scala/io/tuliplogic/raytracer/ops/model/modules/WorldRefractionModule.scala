package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import io.tuliplogic.raytracer.ops.model.data.{Color, Ray, World}
import io.tuliplogic.raytracer.ops.model.modules.PhongReflectionModule.HitComps
import zio.{Ref, RefM, UIO, ZIO}

trait WorldRefractionModule {
  val worldRefractionModule: WorldRefractionModule.Service[Any]
}

object WorldRefractionModule {
  trait Service[R] {
    def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[R, RayTracerError, Color]
  }

  trait Live extends WorldRefractionModule {
    val worldModule: WorldModule.Service[Any]

    override val worldRefractionModule: Service[Any] = new Service[Any] {
      def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] = {

        if (hitComps.shape.material.transparency == 0)
          UIO.succeed(Color.black) // opaque surfaces don't refract
        else {
          val nRatio = hitComps.n1 / hitComps.n2
          val cosTheta_i = (hitComps.eyeV dot hitComps.normalV)
          val sin2Theta_t = nRatio * nRatio * (1 - cosTheta_i * cosTheta_i)
          if (sin2Theta_t > 1) UIO.succeed(Color.black) // total internal reflection reached
          else for {
            r   <- remaining.get
              res <- if (r <= 0) UIO(Color.black) else {
                val cosTheta_t: Double = math.sqrt(1 - sin2Theta_t)
                val direction: Vec = (hitComps.normalV * (nRatio * cosTheta_i - cosTheta_t)) - (hitComps.eyeV * nRatio)
                val refractedRay = Ray(hitComps.underPoint, direction)
                //              println(s"refracted ray: $refractedRay for hp: ${hitComps.hitPt}, eyeV: ${hitComps.eyeV},  normal: ${hitComps.normalV}}")
                worldModule.colorForRay(world, refractedRay, remaining).map(_ * hitComps.shape.material.transparency)
              }
            //          _ <- UIO(println(s"refracted color for r: $res"))
          } yield res
        }
      }
    }
  }

  trait NoRefractionModule extends WorldRefractionModule {
    override val worldRefractionModule: Service[Any] = new Service[Any] {
      override def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[Any, RayTracerError, Color] =
        UIO.succeed(Color.black)
    }
  }

  object > extends WorldRefractionModule.Service[WorldRefractionModule] {
    override def refractedColor(world: World, hitComps: HitComps, remaining: Ref[Int]): ZIO[WorldRefractionModule, RayTracerError, Color] =
      ZIO.accessM(_.worldRefractionModule.refractedColor(world, hitComps, remaining))
  }
}
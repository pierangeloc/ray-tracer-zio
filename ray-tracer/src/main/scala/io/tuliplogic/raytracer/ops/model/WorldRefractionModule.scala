package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import io.tuliplogic.raytracer.ops.drawing.World
import io.tuliplogic.raytracer.ops.model.PhongReflectionModule.HitComps
import zio.{UIO, ZIO}

trait WorldRefractionModule {
  val worldRefractionModule: WorldRefractionModule.Service[Any]
}

object WorldRefractionModule {
  trait Service[R] {
    def refractedColor(world: World, hitComps: HitComps, remaining: Int = 10): ZIO[Any, RayTracerError, Color]
  }

  trait Live extends WorldRefractionModule {
    val worldModule: WorldModule.Service[Any]

    override val worldRefractionModule: Service[Any] = new Service[Any] {
      def refractedColor(world: World, hitComps: HitComps, remaining: Int = 10): ZIO[Any, RayTracerError, Color] = {
        val nRatio = hitComps.n1 / hitComps.n2
        val cosTheta_i = (hitComps.eyeV dot hitComps.normalV)
        val sin2Theta_t = nRatio * nRatio * (1 - cosTheta_i * cosTheta_i)

        if (hitComps.obj.material.transparency == 0) UIO.succeed(Color.black) // opaque surfaces don't refract
        else if (remaining == 0) UIO.succeed(Color.black) // refraction recursion is done
        else if (sin2Theta_t > 1) UIO.succeed(Color.black) // total internal reflection reached
        else {
          val cosTheta_t: Double = math.sqrt(1 - sin2Theta_t)
          val direction: Vec = (hitComps.normalV * (nRatio * cosTheta_i - cosTheta_t)) - (hitComps.eyeV * nRatio)
          val refractedRay = Ray(hitComps.underPoint, direction)
          worldModule.colorForRay(world, refractedRay, remaining - 1).map(_ * hitComps.obj.material.transparency)
        }
      }
    }
  }

  trait NoWorldRefractionModule extends WorldRefractionModule {
    override val worldRefractionModule: Service[Any] = new Service[Any] {
      override def refractedColor(world: World, hitComps: HitComps, remaining: Int): ZIO[Any, RayTracerError, Color] =
        UIO.succeed(Color.black)
    }
  }
}
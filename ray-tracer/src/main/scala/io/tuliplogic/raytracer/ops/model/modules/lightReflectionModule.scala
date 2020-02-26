package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import io.tuliplogic.raytracer.ops.model.data.Color
import io.tuliplogic.raytracer.ops.model.modules.normalReflectModule.NormalReflectModule
import zio.{Has, UIO, ZIO, ZLayer}

/**
  * This module computes the reflective component of the light, i.e. how the light gets reflected by the surface. Depends on specular + shininess
  */
object lightReflectionModule {
  trait Service {
    def reflection(lightV: Vec, normalV: Vec, eyeV: Vec, lightIntensity: Color, materialSpecular: Double, materialShininess: Double): UIO[Color]
  }

  type LightReflectionModule = Has[Service]

  val live: ZLayer[NormalReflectModule, Nothing, LightReflectionModule] =
    ZLayer.fromService { normalReflectSvc =>
      new Service {

        def computeReflection(reflEyeProjection: Double, lightIntensity: Color, materialSpecular: Double, materialShininess: Double): Color = {
          val factor = math.pow(reflEyeProjection, materialShininess)
          lightIntensity * materialSpecular * factor
        }

        def reflection(lightV: Vec, normalV: Vec, eyeV: Vec, lightIntensity: Color, materialSpecular: Double, materialShininess: Double): UIO[Color] =
          for {
            lightReflV <- normalReflectSvc.reflect(-lightV, normalV)
            reflEyeProjection <- UIO(lightReflV dot eyeV)
          } yield if (reflEyeProjection < 0) Color.black else computeReflection(reflEyeProjection, lightIntensity, materialSpecular, materialShininess)
      }
  }

  val noReflection: ZLayer.NoDeps[Nothing, LightReflectionModule] = ZLayer.succeed(
    new Service {
      override def reflection(lightV: Vec, normalV: Vec, eyeV: Vec, lightIntensity: Color, materialSpecular: Double, materialShininess: Double): ZIO[Any, Nothing, Color] = UIO(Color.black)
    })

}
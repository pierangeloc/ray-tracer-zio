package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import zio.{UIO, ZIO}

/**
  * This module computes the reflective component of the light, i.e. how the light gets reflected by the surface. Depends on specular + shininess
  */
trait LightReflectionModule {
  val lightReflectionModule: LightReflectionModule.Service[Any]
}

object LightReflectionModule {
  trait Service[R] {
    def reflection(lightV: Vec, normalV: Vec, eyeV: Vec, lightIntensity: Color, materialSpecular: Double, materialShininess: Double): ZIO[R, Nothing, Color]
  }

  trait Live extends LightReflectionModule {
    val normalReflectModule: NormalReflectModule.Service[Any]

    val lightReflectionModule = new Service[Any] {

      def computeReflection(reflEyeProjection: Double, lightIntensity: Color, materialSpecular: Double, materialShininess: Double): Color = {
        val factor = math.pow(reflEyeProjection, materialShininess)
        lightIntensity * materialSpecular * factor
      }

      override def reflection(lightV: Vec, normalV: Vec, eyeV: Vec, lightIntensity: Color, materialSpecular: Double, materialShininess: Double): ZIO[Any, Nothing, Color] =
        for {
          lightReflV <- normalReflectModule.reflect(-lightV, normalV)
          reflEyeProjection <- UIO(lightReflV dot eyeV)
        } yield if (reflEyeProjection < 0) Color.black else computeReflection(reflEyeProjection, lightIntensity, materialSpecular, materialShininess)
    }
  }

  trait NoReflection extends LightReflectionModule {
    val lightReflectionModule = new Service[Any] {
      override def reflection(lightV: Vec, normalV: Vec, eyeV: Vec, lightIntensity: Color, materialSpecular: Double, materialShininess: Double): ZIO[Any, Nothing, Color] = UIO(Color.black)
    }
  }

}
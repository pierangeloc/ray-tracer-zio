package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.affine.PointVec.Vec
import zio.{UIO, ZIO}

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
        lightIntensity * materialShininess * factor
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
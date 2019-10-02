package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.matrix.MatrixOps
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformationOps
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.PhongReflection.{HitComps, PhongComponents}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
import zio.{UIO, URIO, ZIO}

trait PhongReflection {
  def phongReflectionService: PhongReflection.Service[Any]
}

object PhongReflection {

  case class HitComps(obj: Sphere, pt: Pt, normalV: Vec, eyeV: Vec)

  case class PhongComponents(ambient: Color, diffuse: Color, reflective: Color) {
    def toColor: Color = ambient + diffuse + reflective
  }

  trait Service[R] {

    /**
      * Lighting intensity must result as ambient + diffuse + specular
      * @param pointLight the pointlight that source the illumination
      * @param hitComps the components of the ray hit
      * @return
      */
    def lighting(pointLight: PointLight, hitComps: HitComps): URIO[R, PhongComponents]
  }

  trait Live extends PhongReflection with SpatialEntityOperations {
    override def phongReflectionService: Service[Any] = new Service[Any] {

      override def lighting(pointLight: PointLight, hitComps: HitComps): UIO[PhongComponents] = {

        def diffuseAndReflect(lightV: Vec, effectiveColor: Color, lightDotNormal: Double, ambient: Color): UIO[PhongComponents] =
          for {
            diffuse       <- UIO(effectiveColor * hitComps.obj.material.diffuse * lightDotNormal)
            reflectV      <- spatEntityOperations.reflect(-lightV, hitComps.normalV)
            reflectDotEye <- UIO(reflectV dot hitComps.eyeV)
            refl <- if (reflectDotEye < 0) UIO(Color.black)
            else reflective(reflectDotEye)
          } yield PhongComponents(ambient, diffuse, refl)

        def reflective(reflectDotEye: Double): UIO[Color] =
          for {
            factor   <- UIO(math.pow(reflectDotEye, hitComps.obj.material.shininess))
            specular <- UIO(pointLight.intensity * hitComps.obj.material.specular * factor)
          } yield specular

        for {
          effectiveColor <- UIO.succeed(hitComps.obj.material.color * pointLight.intensity)
          lightV         <- (pointLight.position - hitComps.pt).normalized.orDie
          ambient        <- UIO(effectiveColor * hitComps.obj.material.ambient)
          lightDotNormal <- UIO(lightV dot hitComps.normalV)
          res <- if (lightDotNormal < 0) UIO(PhongComponents(ambient, Color.black, Color.black))
          else diffuseAndReflect(lightV, effectiveColor, lightDotNormal, ambient)
        } yield res
      }
    }
  }

  object Live extends Live with SpatialEntityOperations.Live with AffineTransformationOps.Live with MatrixOps.Live
}

object phongOps extends PhongReflection.Service[PhongReflection] {
  override def lighting(pointLight: PointLight, hitComps: HitComps): URIO[PhongReflection, PhongComponents] = ZIO.accessM(_.phongReflectionService.lighting(pointLight, hitComps))
}

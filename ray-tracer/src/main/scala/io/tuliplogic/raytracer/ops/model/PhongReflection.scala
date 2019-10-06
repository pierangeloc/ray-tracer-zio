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

  case class HitComps(obj: Sphere, pt: Pt, normalV: Vec, eyeV: Vec) {
    def inside: Boolean = (normalV dot eyeV) < 0 //the eye is inside the sphere if the normal vector (pointing always outside) dot eyeV < 0
    def overPoint: Pt = pt + normalV.scale(HitComps.epsilon)
  }

  object HitComps {
    val epsilon: Double = 1e-6
  }

  case class PhongComponents(ambient: Color, diffuse: Color, reflective: Color) {
    def toColor: Color = ambient + diffuse + reflective
  }

  trait Service[R] {

    /**
      * Lighting intensity must result as ambient + diffuse + specular
      * @param pointLight the pointlight that source the illumination
      * @param hitComps the components of the ray hit
      * @param inShadow if the point is in shadow
      * @return
      */
    def lighting(pointLight: PointLight, hitComps: HitComps, inShadow: Boolean): URIO[R, PhongComponents]
  }

  trait Live extends PhongReflection with SpatialEntityOperations {
    override def phongReflectionService: Service[Any] = new Service[Any] {

      override def lighting(pointLight: PointLight, hitComps: HitComps, inShadow: Boolean): UIO[PhongComponents] = {

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

        def computeInShadow(ambient: Color) =
          UIO(PhongComponents(ambient, Color.black, Color.black))

        def computeInLight(ambient: Color, effectiveColor: Color): UIO[PhongComponents] = for {
          lightV         <- (pointLight.position - hitComps.pt).normalized.orDie
          lightDotNormal <- UIO(lightV dot hitComps.normalV)
          res <- if (lightDotNormal < 0) UIO(PhongComponents(ambient, Color.black, Color.black))
          else diffuseAndReflect(lightV, effectiveColor, lightDotNormal, ambient)
        } yield res

        for {
          effectiveColor <- UIO.succeed(hitComps.obj.material.color * pointLight.intensity)
          ambient        <- UIO(effectiveColor * hitComps.obj.material.ambient)
          res <- if (inShadow) computeInShadow(ambient) else computeInLight(ambient, effectiveColor)
        } yield res
      }
    }
  }

  object Live extends Live with SpatialEntityOperations.Live with AffineTransformationOps.Live with MatrixOps.Live
}

object phongOps extends PhongReflection.Service[PhongReflection] {
  override def lighting(pointLight: PointLight, hitComps: HitComps, inShadow: Boolean): URIO[PhongReflection, PhongComponents] =
    ZIO.accessM(_.phongReflectionService.lighting(pointLight, hitComps, inShadow))
}

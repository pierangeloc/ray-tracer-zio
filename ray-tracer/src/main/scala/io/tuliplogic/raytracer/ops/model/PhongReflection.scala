package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformationOps
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.PhongReflection.{HitComps, PhongComponents}
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.PointLight
import zio.{IO, UIO, URIO, ZIO}

trait PhongReflection {
  def phongReflectionService: PhongReflection.Service[Any]
}

object PhongReflection {

  /**
    *
    * @param obj
    * @param pt
    * @param normalV normal vector to the surface of $obj at $pt
    * @param eyeV the eye vector, going from the eye to the surface point
    * @param rayReflectV the reflection vector of the RAY
    */
  case class HitComps(obj: SceneObject, pt: Pt, normalV: Vec, eyeV: Vec, rayReflectV: Vec, n1: Double = 1, n2: Double = 1) {
    def inside: Boolean = (normalV dot eyeV) < 0 //the eye is inside the sphere if the normal vector (pointing always outside) dot eyeV < 0
    def overPoint: Pt   = pt + normalV.*(HitComps.epsilon * (if(inside) -1 else 1))
    def underPoint: Pt  = pt + normalV.*(-HitComps.epsilon * (if(inside) -1 else 1))
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

  //TODO test the phongReflection in terms of the underlying dependencies
  trait BreezeMatrixOps$ extends PhongReflection with SpatialEntityOperations with AffineTransformationOps { self =>
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

        def computeInLight(ambient: Color, effectiveColor: Color): UIO[PhongComponents] =
          for {
            lightV         <- (pointLight.position - hitComps.pt).normalized.orDie
            lightDotNormal <- UIO(lightV dot hitComps.normalV)
            res <- if (lightDotNormal < 0) UIO(PhongComponents(ambient, Color.black, Color.black))
            else diffuseAndReflect(lightV, effectiveColor, lightDotNormal, ambient)
          } yield res

        def computeColor: IO[AlgebraicError, Color] =
          for {
            objectTf     <- UIO(hitComps.obj.transformation)
            objectTfInv  <- affineTfOps.invert(objectTf)
            patternTf    <- UIO(hitComps.obj.material.pattern.transformation)
            patternTfInv <- affineTfOps.invert(patternTf)
            composed     <- (objectTfInv >=> patternTfInv).provide(self)
            effectivePt  <- affineTfOps.transform(composed, hitComps.pt)
          } yield hitComps.obj.material.pattern(effectivePt)

        for {
          color          <- computeColor.orDie
          effectiveColor <- UIO.succeed(color * pointLight.intensity)
          ambient        <- UIO(effectiveColor * hitComps.obj.material.ambient)
          res            <- if (inShadow) computeInShadow(ambient) else computeInLight(ambient, effectiveColor)
        } yield res
      }
    }
  }

  object BreezeMatrixOps$ extends BreezeMatrixOps$ with SpatialEntityOperations.BreezeMatrixOps$ with AffineTransformationOps.BreezeMatrixOps$ with MatrixModule.BreezeMatrixModule
}

object phongOps extends PhongReflection.Service[PhongReflection] {
  override def lighting(pointLight: PointLight, hitComps: HitComps, inShadow: Boolean): URIO[PhongReflection, PhongComponents] =
    ZIO.accessM(_.phongReflectionService.lighting(pointLight, hitComps, inShadow))
}

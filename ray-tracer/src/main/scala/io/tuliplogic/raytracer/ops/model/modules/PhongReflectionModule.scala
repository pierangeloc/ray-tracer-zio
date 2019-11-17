package io.tuliplogic.raytracer.ops.model.modules

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.data.Color
import io.tuliplogic.raytracer.ops.model.data.Scene.{PointLight, Shape}
import zio.{IO, UIO, URIO, ZIO}

trait PhongReflectionModule {
  val phongReflectionModule: PhongReflectionModule.Service[Any]
}

object PhongReflectionModule {

  /**
    *
    * @param shape
    * @param hitPt
    * @param normalV normal vector to the surface of $obj at $pt
    * @param eyeV the eye vector, going from the eye to the surface point
    * @param rayReflectV the reflection vector of the RAY
    */
  case class HitComps(shape: Shape, hitPt: Pt, normalV: Vec, eyeV: Vec, rayReflectV: Vec, n1: Double = 1, n2: Double = 1) {
    def inside: Boolean = (normalV dot eyeV) < 0 //the eye is inside the sphere if the normal vector (pointing always outside) dot eyeV < 0
    def overPoint: Pt   = hitPt + normalV.*(HitComps.epsilon * (if(inside) -1 else 1))
    def underPoint: Pt  = hitPt + normalV.*(-HitComps.epsilon * (if(inside) -1 else 1))
  }

  object HitComps {
    val epsilon: Double = 1e-6
  }

  case class PhongComponents(ambient: Color, diffuse: Color, reflective: Color) {
    def toColor: Color = ambient + diffuse + reflective
    def +(other: PhongComponents): PhongComponents = PhongComponents(ambient + other.ambient, diffuse + other.diffuse, reflective + other.reflective)
  }

  object PhongComponents {
    def allBlack = PhongComponents(Color.black, Color.black, Color.black) //the zero of PhongComponents monoid
    def allWhite = PhongComponents(Color.white, Color.white, Color.white) //the zero of PhongComponents monoid

    def ambient(c: Color) = PhongComponents(c, Color.black, Color.black)
    def diffuse(c: Color) = PhongComponents(Color.black, c, Color.black)
    def reflective(c: Color) = PhongComponents(Color.black, Color.black, c)
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
  trait Live extends PhongReflectionModule {

    val aTModule: ATModule.Service[Any]
    val normalReflectModule: NormalReflectModule.Service[Any]
    val lightDiffusionModule: LightDiffusionModule.Service[Any]
    val lightReflectionModule: LightReflectionModule.Service[Any]

    override val phongReflectionModule: Service[Any] = new Service[Any] {

      override def lighting(pointLight: PointLight, hitComps: HitComps, inShadow: Boolean): UIO[PhongComponents] = {

        def colorAtSurfacePoint: UIO[Color] =
          for {
            objectTf     <- UIO(hitComps.shape.transformation)
            objectTfInv  <- UIO.succeed(objectTf.inverted)
            patternTf    <- UIO(hitComps.shape.material.pattern.transformation)
            patternTfInv <- UIO.succeed(patternTf.inverted)
            composed     <- aTModule.compose(objectTfInv, patternTfInv)
            effectivePt  <- aTModule.applyTf(composed, hitComps.hitPt)
          } yield hitComps.shape.material.pattern(effectivePt)

        def lightV: UIO[Vec] = (pointLight.position - hitComps.hitPt).normalized.orDie

        def diffuseAndRefl(effectiveColor: Color): UIO[PhongComponents] = lightV.flatMap{ lv =>
            lightDiffusionModule.diffusion(effectiveColor, hitComps.shape.material.diffuse, lv, hitComps.normalV).zipPar(
            lightReflectionModule.reflection(lv, hitComps.normalV, hitComps.eyeV, pointLight.intensity, hitComps.shape.material.specular, hitComps.shape.material.shininess)
          ).map {
            case (d, r) => PhongComponents.diffuse(d) + PhongComponents.reflective(r)
          }
        }

        for {
          color          <- colorAtSurfacePoint
          effectiveColor <- UIO.succeed(color * pointLight.intensity)
          ambient        <- UIO(PhongComponents.ambient(effectiveColor * hitComps.shape.material.ambient))
          res            <- if (inShadow) UIO(PhongComponents.allBlack) else diffuseAndRefl(effectiveColor)
          } yield ambient + res
      }
    }
  }

  trait BlackWhite extends PhongReflectionModule {
    override val phongReflectionModule: Service[Any] = new Service[Any] {

      override def lighting(pointLight: PointLight, hitComps: HitComps, inShadow: Boolean): UIO[PhongComponents] = {
        if (inShadow) UIO(PhongComponents.diffuse(Color.black))
        else UIO(PhongComponents(Color.black, Color.white, Color.black))
      }
    }
  }

  object > extends PhongReflectionModule.Service[PhongReflectionModule] {
    override def lighting(pointLight: PointLight, hitComps: HitComps, inShadow: Boolean): URIO[PhongReflectionModule, PhongComponents] =
      ZIO.accessM(_.phongReflectionModule.lighting(pointLight, hitComps, inShadow))
  }

}


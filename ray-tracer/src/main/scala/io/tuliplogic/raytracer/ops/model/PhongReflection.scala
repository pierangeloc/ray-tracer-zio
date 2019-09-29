package io.tuliplogic.raytracer.ops.model

import io.tuliplogic.raytracer.geometry.matrix.MatrixOps
import io.tuliplogic.raytracer.geometry.vectorspace.AffineTransformationOps
import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.PhongReflection.PhongComponents
import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.PointLight
import zio.{UIO, URIO, ZIO}

trait PhongReflection {
  def phongReflectionService: PhongReflection.Service[Any]
}

object PhongReflection {

  case class PhongComponents(ambient: Color, diffuse: Color, reflective: Color) {
    def toIntensity: Color = ambient + diffuse + reflective
  }

  trait Service[R] {

    /**
      * Lighting intensity must result as ambient + diffuse + specular
      * @param material the material of the point
      * @param pointLight the pointlight that source the illumination
      * @param surfacePoint the point of the surface hit by the ray trace
      * @param eyeVector the vector between the point of view and the point under exam
      * @param normalVector the normal vector to the surface
      * @return
      */
    def lighting(material: Material, pointLight: PointLight, surfacePoint: Pt, eyeVector: Vec, normalVector: Vec): URIO[R, PhongComponents]
  }

  trait Live extends PhongReflection with SpatialEntityOperations {
    override def phongReflectionService: Service[Any] = new Service[Any] {

      override def lighting(
          material: Material,
          pointLight: PointLight,
          surfacePoint: Pt,
          eyeVector: Vec,
          normalVector: Vec
      ): UIO[PhongComponents] = {

        def diffuseAndReflect(lightV: Vec, effectiveColor: Color, lightDotNormal: Double, ambient: Color): UIO[PhongComponents] =
          for {
            diffuse       <- UIO(effectiveColor * material.diffuse * lightDotNormal)
            reflectV      <- spatEntityOperations.reflect(-lightV, normalVector)
            reflectDotEye <- UIO(reflectV dot eyeVector)
            refl <- if (reflectDotEye < 0) UIO(Color.black)
            else reflective(reflectDotEye)
          } yield PhongComponents(ambient, diffuse, refl)

        def reflective(reflectDotEye: Double): UIO[Color] =
          for {
            factor   <- UIO(math.pow(reflectDotEye, material.shininess))
            specular <- UIO(pointLight.intensity * material.specular * factor)
          } yield Color.white * specular

        for {
          effectiveColor <- UIO.succeed(material.color * pointLight.intensity)
          lightV         <- (pointLight.position - surfacePoint).normalized.orDie
          ambient        <- UIO(effectiveColor * material.ambient)
          lightDotNormal <- UIO(lightV dot normalVector)
          res <- if (lightDotNormal < 0) UIO(PhongComponents(ambient, Color.black, Color.black))
          else diffuseAndReflect(lightV, effectiveColor, lightDotNormal, ambient)
        } yield res
      }
    }
  }

  object Live extends Live with SpatialEntityOperations.Live with AffineTransformationOps.Live with MatrixOps.Live
}

object phong extends PhongReflection.Service[PhongReflection] {
  override def lighting(
      material: Material,
      pointLight: PointLight,
      surfacePoint: Pt,
      eyeVector: Vec,
      normalVector: Vec
  ): URIO[PhongReflection, PhongComponents] = ZIO.accessM(_.phongReflectionService.lighting(material, pointLight, surfacePoint, eyeVector, normalVector))
}

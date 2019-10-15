package io.tuliplogic.raytracer.ops.drawing

  import io.tuliplogic.raytracer.ops.model.{Color, Intersection, PhongReflection, Ray, RayOperations, SpatialEntityOperations, phongOps, rayOps, spatialEntityOps}
  import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject.{PointLight, Sphere}
  import io.tuliplogic.raytracer.commons.errors.{AlgebraicError, BusinessError, RayTracerError}
  import io.tuliplogic.raytracer.ops.model.PhongReflection.HitComps
  import zio.{IO, UIO, URIO, ZIO}
  import cats.implicits._
  import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.{Pt, Vec}
  import io.tuliplogic.raytracer.ops.model.SpatialEntity.SceneObject
  import zio.interop.catz._
  import Predef.{any2stringadd => _,_}

case class World(pointLight: PointLight, objects: List[SceneObject]) {
  def intersect(ray: Ray): URIO[RayOperations, List[Intersection]] =
    objects.traverse(rayOps.intersect(ray, _)).map(_.flatten.sortBy(_.t))

  def colorAt(ray: Ray, remaining: Int = 5): ZIO[PhongReflection with RayOperations with SpatialEntityOperations, RayTracerError, Color] =
    for {
      intersections <- intersect(ray)
      maybeHitComps <- intersections.find(_.t > 0).traverse(World.hitComps(ray, _, intersections))
      color <- maybeHitComps
        .map(hc =>
          for {
            shadowed <- isShadowed(hc.overPoint)
            color <- phongOps.lighting(pointLight, hc, shadowed).map(_.toColor)
            //invoke this only if remaining > 0. Also, reflected color and color can be computed in parallel
            reflectedColor <- if (remaining > 0) World.reflectedColor(this, hc, remaining - 1) else UIO(Color.black)
            refractedColor <- if (remaining > 0) World.refractedColor(this, hc, remaining) else UIO(Color.black)
          } yield color + reflectedColor
        ).getOrElse(UIO(Color.black))
    } yield color

  def isShadowed(pt: Pt): ZIO[RayOperations, AlgebraicError, Boolean] =
    for {
      v        <- UIO(pointLight.position - pt)
      distance <- v.norm
      vNorm    <- v.normalized
      xs       <- intersect(Ray(pt, vNorm))
      hit      <- rayOps.hit(xs)
    } yield hit.exists(i => i.t > 0 && i.t < distance)
}

object World {
  //TODO: move all these things into a service, no methods on the world. Make World just data, plus some syntax for utility

  def hitComps(
      ray: Ray,
      hit: Intersection,
      intersections: List[Intersection]
  ): ZIO[SpatialEntityOperations with RayOperations, BusinessError.GenericError, HitComps] = {

    type Z = (List[SceneObject], Option[Double], Option[Double])

    /**
      * We can calculate the n1, n2 for the hit, given the list of intersections. Each intersection carries the object, together with its material
      * @return
      */
    def n1n2: UIO[(Double, Double)] = {

      val maybeN1N2: (List[SceneObject], Option[Double], Option[Double]) = intersections.foldLeft[Z]((Nil, None, None)) {
        case (in@(xs, Some(n1), Some(n2)), _) =>
          in
        case ((conts, None, None), `hit`) =>
          val n1: Double = conts.lastOption.map(_.material.refractionIndex).getOrElse(1)
          val contss =
            if (conts.contains(hit.sceneObject))
            conts.filter(_ != hit.sceneObject)
              else
            conts :+ hit.sceneObject
          val n2: Double = contss.lastOption.map(_.material.refractionIndex).getOrElse(1)
          (contss, Some(n1), Some(n2))

        case ((conts, None, None), i) =>
          val contss = if (conts.contains(i.sceneObject)) conts.filter(_ != i.sceneObject)
          else conts :+ i.sceneObject
          (contss, None, None)

        case _ => (Nil, None, None)
      }

      maybeN1N2 match {
        case (_, Some(n1), Some(n2)) => UIO.succeed((n1, n2))
        case _ => ZIO.die(new IllegalArgumentException("can't determine refraction indexes")) //TODO: proper error mgmgt
      }

    }

    for {
      pt       <- rayOps.positionAt(ray, hit.t)
      normalV  <- spatialEntityOps.normal(pt, hit.sceneObject)
      eyeV     <- UIO(-ray.direction)
      reflectV <- spatialEntityOps.reflect(ray.direction, normalV)
      (n1, n2) <- n1n2
    } yield HitComps(hit.sceneObject, pt, normalV, eyeV, reflectV, n1, n2)
  }


  //TODO: make remaining part of an environment initialized with Ref
  def reflectedColor(world: World, hitComps: HitComps, remaining: Int = 10): ZIO[PhongReflection with RayOperations with SpatialEntityOperations, RayTracerError, Color] =
    if (hitComps.obj.material.reflective == 0) {
      UIO(Color.black)
    } else {
      val reflRay = Ray(hitComps.overPoint, hitComps.rayReflectV)
      world.colorAt(reflRay, remaining).map(c =>
        c * hitComps.obj.material.reflective
      )
    }

  def refractedColor(world: World, hitComps: HitComps, remaining: Int = 10): ZIO[PhongReflection with RayOperations with SpatialEntityOperations, RayTracerError, Color] = {
    val nRatio = hitComps.n1 / hitComps.n2
    val eyeDotNormal = (hitComps.eyeV dot hitComps.normalV)
    val cosTheta_i = eyeDotNormal * eyeDotNormal
    val sin2Theta_t = nRatio * nRatio * (1 - cosTheta_i * cosTheta_i)

    if (hitComps.obj.material.transparency == 0) UIO.succeed(Color.black) // opaque surfaces don't refract
    else if (remaining == 0) UIO.succeed(Color.black) // refraction recursion is done
    else if (sin2Theta_t > 1) UIO.succeed(Color.black) // total internal reflection reached
    else {
      val cosTheta_t: Double = math.sqrt(1 - sin2Theta_t)
      val direction: Vec = (hitComps.normalV * (nRatio * cosTheta_i - cosTheta_t)) + (hitComps.eyeV * nRatio)
      world.colorAt(Ray(hitComps.underPoint, direction), remaining - 1)
    }
  }
}

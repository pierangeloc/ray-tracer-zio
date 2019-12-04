package io.tuliplogic.raytracer.http.model

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.commons.errors.BusinessError.GenericError
import io.tuliplogic.raytracer.commons.errors.IOError.HttpError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.http.model.Shape.{Plane, Sphere}
import io.tuliplogic.raytracer.ops.model.data
import io.tuliplogic.raytracer.ops.model.data.{Color, World}
import zio.{UIO, ZIO}

object Http2World {
  case class SceneBundle(
      world: World,
      viewFrom: Pt,
      viewTo: Pt,
      viewUp: Vec,
      visualAngleRad: Double,
      hRes: Int,
      vRes: Int
  )

  private def viewFrom(httpCamera: Camera): Pt =
    Pt(httpCamera.fromX, httpCamera.fromY, httpCamera.fromZ)
  private def viewTo(httpCamera: Camera): Pt = Pt(httpCamera.toX, httpCamera.toY, httpCamera.toZ)
  private def upVec(httpCamera: Camera): Vec = Vec(httpCamera.upX, httpCamera.upY, httpCamera.upZ)
  private def hRes(httpCamera: Camera): Int  = httpCamera.hRes
  private def vRes(httpCamera: Camera): Int  = httpCamera.vRes

  private def pattern(pattern: Pattern): ZIO[ATModule, GenericError, data.Pattern] = pattern match {
    case Pattern.Striped(c1, c2, stripSize) =>
      for {
        col1 <- Color.fromHex(c1)
        col2 <- Color.fromHex(c2)
        tf   <- ATModule.>.scale(stripSize, 1, 1)
      } yield data.Pattern.Striped(col1, col2, tf)

    case Pattern.Checker(c1, c2, size: Double) =>
      for {
        col1 <- Color.fromHex(c1)
        col2 <- Color.fromHex(c2)
        tf   <- ATModule.>.scale(size, size, size)
      } yield data.Pattern.Checker(col1, col2, tf)

    case Pattern.Uniform(c: String) =>
      for {
        col <- Color.fromHex(c)
        tf  <- ATModule.>.id
      } yield data.Pattern.Uniform(col, tf)
  }

  private def material(mat: Material): ZIO[ATModule, GenericError, data.Material] = mat match {
    case Material(p, ambient, diffuse, specular, shininess, reflective, transparency, refractionIndex) =>
      for {
        p <- pattern(p)
      } yield
        data.Material(
          p,
          ambient.getOrElse(0),
          diffuse.getOrElse(0),
          specular.getOrElse(0),
          shininess.getOrElse(0),
          reflective.getOrElse(0),
          transparency.getOrElse(0),
          refractionIndex.getOrElse(1)
        )
  }

  private def httpShape2Shape(s: Shape): ZIO[ATModule, RayTracerError, data.Scene.Shape] = s match {
    case Plane(m, rotateX, rotateY, rotateZ, translateX, translateY, translateZ) =>
      for {
        mat <- material(m)
        plane <- data.Scene.Plane.make(
          rotateX.getOrElse(0),
          rotateY.getOrElse(0),
          rotateZ.getOrElse(0),
          Pt(translateX.getOrElse(0), translateY.getOrElse(0), translateZ.getOrElse(0)),
          mat)
      } yield plane

    case Sphere(m, centerX, centerY, centerZ, radius) =>
      for {
        mat <- material(m)
        s <- data.Scene.Sphere.make(
          Pt(centerX, centerY, centerZ),
          radius,
          mat
        )
      } yield s
  }

  def httpScene2World(httpScene: Scene): ZIO[ATModule, HttpError, SceneBundle] = (
    for {
      worldShapes     <- ZIO.traverse(httpScene.shapes)(httpShape2Shape).mapError(e => HttpError(e.getMessage))
      pointLightColor <- Color.fromHex(httpScene.pointLight.color).mapError(e => HttpError(e.getMessage))
      pointLight      <- UIO(data.Scene.PointLight(Pt(httpScene.pointLight.ptX, httpScene.pointLight.ptY, httpScene.pointLight.ptZ), pointLightColor))

    } yield
      SceneBundle(
        World(pointLight, worldShapes),
        viewFrom(httpScene.camera),
        viewTo(httpScene.camera),
        upVec(httpScene.camera),
        math.Pi / 3,
        httpScene.camera.hRes,
        httpScene.camera.vRes
      )
  )

}

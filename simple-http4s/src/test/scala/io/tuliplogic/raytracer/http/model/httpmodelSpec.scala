package io.tuliplogic.raytracer.http.model

import zio.ZIO
import zio.test._
import zio.test.Assertion._
import io.circe.generic.auto._
import io.circe.syntax._
import io.tuliplogic.raytracer.http.model.Shape.Sphere

object httpmodelSpec extends DefaultRunnableSpec(
  suite("parse correctly body") {
    test("scene with 2 spheres is decodable") {
      val mat1 = Material(
        Pattern.Striped("ff0000", "00ff00", 5),
        None,
        None,
        None,
        None,
        None,
        None,
        None
      )
      val s1 = Sphere(mat1, 5, 5, 5, 5)
      val s2 = Sphere(mat1.copy(pattern = Pattern.Checker("ff0000", "00ff00", 5)), 5, 5, 5, 5)
      val camera = Camera(
        fromX = 7, fromY = 7, fromZ = 7,
        toX = 0, toY = 0, toZ = 0,
        upX = 0, upY = 0, upZ = 1,
        hRes = 640, vRes = 480
      )
      val scene = Scene(List(s1, s2), PointLight(10, 10, 10, "ff0000"), camera)
      println(scene.asJson.spaces2)
      import io.circe.parser._

      assert(parse(scene.asJson.spaces2).flatMap(_.as[Scene]), equalTo(Right(scene)))
    }
  }
)

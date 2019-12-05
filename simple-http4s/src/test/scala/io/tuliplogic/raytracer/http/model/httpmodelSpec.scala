package io.tuliplogic.raytracer.http.model

import zio.test._
import zio.test.Assertion._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._
import io.tuliplogic.raytracer.http.model.Shape.Sphere

object httpmodelSpec extends DefaultRunnableSpec(
  suite("parse correctly body") (
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

      assert(parse(scene.asJson.spaces2).flatMap(_.as[Scene]), equalTo(Right(scene)))
    },
    test("parse world") {
      val world = """{
                    |  "shapes" : [
                    |    {
                    |      "Plane" : {
                    |        "material" : {
                    |          "pattern" : {
                    |            "Checker" : {
                    |              "c1" : "cccccc",
                    |              "c2" : "333333",
                    |              "size" : 4.0
                    |            }
                    |          },
                    |          "ambient" : 0.1,
                    |          "diffuse" : 0.9,
                    |          "specular" : 0.9,
                    |          "shininess" : 200,
                    |          "reflective" : 0,
                    |          "transparency" : 0,
                    |          "refractionIndex" : 1
                    |        }
                    |      }
                    |    },
                    |    {
                    |      "Plane" : {
                    |        "material" : {
                    |          "pattern" : {
                    |            "Checker" : {
                    |              "c1" : "cccccc",
                    |              "c2" : "333333",
                    |              "size" : 4.0
                    |            }
                    |          },
                    |          "ambient" : 0.1,
                    |          "diffuse" : 0.9,
                    |          "specular" : 0.9,
                    |          "shininess" : 200,
                    |          "reflective" : 0,
                    |          "transparency" : 0,
                    |          "refractionIndex" : 1
                    |        },
                    |        "rotateX": 1.57,
                    |        "rotateY": -1.57,
                    |        "translateX": -10
                    |      }
                    |    },
                    |    {
                    |      "Sphere" : {
                    |        "material" : {
                    |          "pattern" : {
                    |            "GradientX" : {
                    |              "c1" : "f07931",
                    |              "c2" : "91b357"
                    |            }
                    |          },
                    |          "ambient" : 0.1,
                    |          "diffuse" : 0.9,
                    |          "specular" : 0.9,
                    |          "shininess" : 200,
                    |          "reflective" : 0,
                    |          "transparency" : 0,
                    |          "refractionIndex" : 1
                    |        },
                    |        "centerX" : 5.0,
                    |        "centerY" : 2.0,
                    |        "centerZ" : 5.0,
                    |        "radius" : 2.0
                    |      }
                    |    },
                    |    {
                    |      "Sphere" : {
                    |        "material" : {
                    |          "pattern" : {
                    |            "Striped" : {
                    |              "c1" : "ff0000",
                    |              "c2" : "00ff00",
                    |              "stripSize" : 0.3
                    |            }
                    |          },
                    |          "ambient" : 0.1,
                    |          "diffuse" : 0.7,
                    |          "specular" : 0.3,
                    |          "shininess" : 200,
                    |          "reflective" : 0,
                    |          "transparency" : 0,
                    |          "refractionIndex" : 1
                    |        },
                    |        "centerX" : 10.0,
                    |        "centerY" : 4.0,
                    |        "centerZ" : 8.0,
                    |        "radius" : 3.0
                    |      }
                    |    }
                    |  ],
                    |  "pointLight" : {
                    |    "ptX" : 10.0,
                    |    "ptY" : 10.0,
                    |    "ptZ" : 2.0,
                    |    "color" : "ffffff"
                    |  },
                    |  "camera" : {
                    |    "fromX" : 12.0,
                    |    "fromY" : 4.0,
                    |    "fromZ" : 0.0,
                    |    "toX" : 0.0,
                    |    "toY" : 4.0,
                    |    "toZ" : 16.0,
                    |    "upX" : 0.0,
                    |    "upY" : 1.0,
                    |    "upZ" : 0.0,
                    |    "hRes" : 640,
                    |    "vRes" : 480
                    |  }
                    |}""".stripMargin

      val res = parse(world).flatMap(_.as[Scene])
      println(res)
      assert(res.isRight, equalTo(true))
    }
  )
)

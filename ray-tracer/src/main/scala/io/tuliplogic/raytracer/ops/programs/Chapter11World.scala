package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.data.Scene.{Plane, PointLight, Shape, Sphere}
import io.tuliplogic.raytracer.ops.model.data.{Color, Material, Pattern, World}
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio.{App, UIO, ZEnv, ZIO, console}


object Chapter11World extends App {
  val canvasFile    = "ppm/chapter-11-reflective-spheres" + System.currentTimeMillis + ".ppm"
  val path: Path = Paths.get(canvasFile)
  val lightPosition = Pt(10, 10, 2)
  val cameraFrom    = Pt(9, 4, -4)
  val cameraTo      = Pt(0, 4, 16)
  val cameraUp      = Vec(0, 1, 0)

  val (hRes, vRes) = (640, 480)
//  val (hRes, vRes) = (100, 50)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasSerializer.PPMCanvasSerializer with FullModules
      }
      .foldM(err => console.putStrLn(s"Execution failed with: ${err.getStackTraceString}").as(1), _ => UIO.succeed(0))

  val world: ZIO[ATModule, AlgebraicError, World] = for {
    mat      <- Material.default
    idTf     <- ATModule.>.id
    scale4Tf <- ATModule.>.scale(4, 4, 4)
    planeMat <- UIO(mat.copy(pattern = Pattern.Checker(Color(230/256.0, 29/256.0, 31/256.0), Color(0.9, 0.9, 0.9), scale4Tf), specular = 0, reflective = 0.1))
    floorS   <- Plane.canonical.map(_.copy(material = planeMat)) //grey, matte

    leftWallTf2 <- ATModule.>.rotateX(math.Pi / 2)
    leftWallTf3 <- ATModule.>.rotateY(-math.Pi / 2)
    leftWallTf4 <- ATModule.>.translate(-10, 0, 0)
    leftWallTf  <- ATModule.>.compose(leftWallTf2, leftWallTf3).flatMap(ATModule.>compose(_, leftWallTf4))
    leftWallS   <- UIO(Plane(leftWallTf, planeMat))

    s1Tf1 <- ATModule.>.translate(5, 2, 5)
    s1Tf2 <- ATModule.>.scale(2, 2, 2)
    s1Tf  <- ATModule.>.compose(s1Tf2, s1Tf1)
    s1 <- UIO(
      Sphere(s1Tf, mat.copy(pattern = Pattern.Uniform(Color(240 / 256.0, 121 / 256.0, 49 / 256.0), idTf), diffuse = 0.7, specular = 0.3)))

    s2Tf1 <- ATModule.>.translate(10, 4, 8)
    s2Tf2 <- ATModule.>.scale(3, 3, 3)
    s2Tf  <- ATModule.>.compose(s2Tf2, s2Tf1)
    p2Tf1 <- ATModule.>.scale(0.3, 0.3, 0.3)
    p2Tf2 <- ATModule.>.rotateZ(math.Pi / 4)
    p2Tf  <- ATModule.>.compose(p2Tf1, p2Tf2)
    s2 <- UIO(
      Sphere(
        s2Tf,
        mat.copy(
          pattern = Pattern.Striped(Color.blue, Color.red, p2Tf),
          diffuse = 0.7,
          specular = 0.3,
          reflective = 0.1
        )
      )
    )

    s3Tf1 <- ATModule.>.translate(7, 4, 4)
    s3Tf2 <- ATModule.>.scale(2, 2, 1)
    s3Tf  <- ATModule.>.compose(s3Tf2, s3Tf1)
    glass <- Material.glass
    s3 <- UIO(
      Sphere(
        s3Tf,
        glass
      )
    )

  } yield World(PointLight(lightPosition, Color.white), List[Shape](
//    s1, s2,
    s3, floorS, leftWallS))

  val program = for {
    w      <- world
      canvas <- RaytracingProgram.drawOnCanvas(w, cameraFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
      _      <- CanvasSerializer.>.serializeToFile(canvas, 255, path)
  } yield ()
}

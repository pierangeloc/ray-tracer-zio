package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.AlgebraicError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.data.Scene.{Plane, PointLight, Shape, Sphere}
import io.tuliplogic.raytracer.ops.model.data.{Color, Material, Pattern, World}
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio.{App, UIO, ZEnv, ZIO, console}

object Chapter10World2 extends App {
  val canvasFile    = "ppm/chapter-10-two-spheres-shadow-" + System.currentTimeMillis + ".ppm"
  val lightPosition = Pt(10, 10, 2)
  val cameraFrom    = Pt(12, 4, 0)
  val cameraTo      = Pt(0, 4, 16)
  val cameraUp      = Vec(0, 1, 0)

  val (hRes, vRes) = (640, 480)
//  val (hRes, vRes) = (100, 50)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasSerializer.PPMCanvasSerializer with ATModule.Live
        with FullModules {
          override def path: Path = Paths.get(canvasFile)
        }
      }.timed
      .foldM(err =>
        console.putStrLn(s"Execution failed with: ${err.getStackTraceString}").as(1),
        {case (duration, _) => console.putStrLn(s"it took ${duration.toMillis}") *> UIO.succeed(0)})

  //TODO: make a DSL to build a world, this is too painful

  val world: ZIO[ATModule, AlgebraicError, World] = for {
    mat      <- Material.default
    idTf     <- ATModule.>.id
    scale4   <- ATModule.>.scale(4, 4, 4)
    wallMat <- UIO(mat.copy(pattern = Pattern.Checker(Color(0.8, 0.8, 0.8), Color(0.2, 0.2, 0.2), scale4), specular = 0))
//    wallMat <- UIO(mat.copy(pattern = Pattern.Ring(Color(0.8, 0.8, 0.8), Color(0.2, 0.2, 0.2), scale4), specular = 0))
//    wallMat <- UIO(mat.copy(pattern = Pattern.Uniform(Color(0.8, 0.8, 0.8), scale4), specular = 0))
//    floorMat <- UIO(mat.copy(pattern = Pattern.Ring(Color(1, 0.9, 0.9), Color(0.9, 1, 0.9), idTf), specular = 0))
    floorS <- Plane.canonical.map(_.copy(material = wallMat)) //grey, matte

    leftWallTf2 <- ATModule.>.rotateX(math.Pi / 2)
    leftWallTf3 <- ATModule.>.rotateY(-math.Pi / 2)
    leftWallTf4 <- ATModule.>.translate(-10, 0, 0)
    leftWallTf  <- ATModule.>.compose(leftWallTf2, leftWallTf3).flatMap(ATModule.>.compose(_, leftWallTf4))
    leftWallS   <- UIO(Plane(leftWallTf, wallMat))

    s1Tf1 <- ATModule.>.translate(5, 2, 5)
    s1Tf2 <- ATModule.>.scale(2, 2, 2)
//    p1Tf <- ATModule.>.scale(0.2, 0.2, 0.2)
    s1Tf <- ATModule.>.compose(s1Tf2, s1Tf1)
    s1 <- UIO(
      Sphere(
        s1Tf,
        mat.copy(
          pattern = Pattern.GradientX(Color(240 / 256.0, 121 / 256.0, 49 / 256.0), Color(145 / 256.0, 179 / 256.0, 87 / 256.0), idTf),
          diffuse = 0.7,
          specular = 0.9,
          shininess = 50)))

    s2Tf1 <- ATModule.>.translate(10, 4, 8)
    s2Tf2 <- ATModule.>.scale(3, 3, 3)
    s2Tf  <- ATModule.>.compose(s2Tf2, s2Tf1)
    p2Tf1 <- ATModule.>.scale(0.3, 0.3, 0.3)
    p2Tf2 <- ATModule.>.rotateZ(math.Pi / 4)
    p2Tf  <- ATModule.>.compose(p2Tf1, p2Tf2)
    s2    <- UIO(Sphere(s2Tf, mat.copy(pattern = Pattern.Striped(Color.blue, Color.red, p2Tf), diffuse = 0.7, specular = 0.3)))

  } yield World(PointLight(lightPosition, Color.white), List[Shape](s1, s2, floorS, leftWallS))

  val program = for {
    w      <- world
      canvas <- RaytracingProgram.drawOnCanvas(w, cameraFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
      _      <- CanvasSerializer.>.serialize(canvas, 255)
  } yield ()

}

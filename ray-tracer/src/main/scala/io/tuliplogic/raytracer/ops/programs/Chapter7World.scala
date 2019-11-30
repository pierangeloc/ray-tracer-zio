package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.data.Scene.{PointLight, Sphere}
import io.tuliplogic.raytracer.ops.model.data.{Color, Material, Pattern, World}
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio.{App, UIO, ZEnv, ZIO, console}

object Chapter7World extends App {
  val canvasFile    = "ppm/chapter-7-three-spheres-shadow-" + System.currentTimeMillis + ".ppm"
  val lightPosition = Pt(-10, 5, -10)
  val cameraFrom    = Pt(0, 1.5, -5)
  val cameraTo      = Pt(0, 1, 0)
  val cameraUp      = Vec(0, 1, 0)

  val (hRes, vRes) = (640, 480)

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasSerializer.PPMCanvasSerializer with FullModules {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: $err").as(1), _ => UIO.succeed(0))


  val world = for {
    defaultMat   <- Material.default
    idTf         <- ATModule.>.id
    mat          <- UIO(defaultMat.copy(pattern = Pattern.Uniform(Color(1, 0.9, 0.9), idTf), specular = 0))
    floorTf      <- ATModule.>.scale(10, 0.01, 10) //very flat ellipsoid
    floorS       <- UIO(Sphere(floorTf, mat)) //grey, matte
    leftWallTf1  <- ATModule.>.scale(10, 0.01, 10)
    leftWallTf2  <- ATModule.>.rotateX(math.Pi / 2)
    leftWallTf3  <- ATModule.>.rotateY(-math.Pi / 4)
    leftWallTf4  <- ATModule.>.translate(0, 0, 5)
    leftWallTf   <- ATModule.>.compose(leftWallTf1, leftWallTf2).flatMap(ATModule.>.compose(_, leftWallTf3)).flatMap(ATModule.>.compose(_, leftWallTf4))
    leftWallS    <- UIO(Sphere(leftWallTf, mat))
    rightWallTf1 <- ATModule.>.scale(10, 0.01, 10)
    rightWallTf2 <- ATModule.>.rotateX(math.Pi / 2)
    rightWallTf3 <- ATModule.>.rotateY(math.Pi / 4)
    rightWallTf4 <- ATModule.>.translate(0, 0, 5)
    rightWallTf  <- ATModule.>.compose(rightWallTf1, rightWallTf2).flatMap(ATModule.>.compose(_, rightWallTf3)).flatMap(ATModule.>.compose(_,  rightWallTf4))
    rightWallS   <- UIO(Sphere(rightWallTf, mat))
    s1Tf         <- ATModule.>.translate(-0.5, 1.2, 0.5)
    s1           <- UIO(Sphere(s1Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(0.1, 1, 0.5), idTf), diffuse = 0.7, specular = 0.3)))
    s2Tf1        <- ATModule.>.scale(0.5, 0.5, 0.5)
    s2Tf2        <- ATModule.>.translate(1.5, 0.5, -0.5)
    s2Tf         <- ATModule.>.compose(s2Tf2,  s2Tf1)
    s2           <- UIO(Sphere(s2Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(0.5, 1, 0.1), idTf), diffuse = 0.7, specular = 0.3)))
    s3Tf1        <- ATModule.>.scale(0.33, 0.33, 0.33)
    s3Tf2        <- ATModule.>.translate(-1.5, 0.33, -0.75)
    s3Tf         <- ATModule.>.compose(s3Tf2, s3Tf1)
    s3           <- UIO(Sphere(s3Tf, defaultMat.copy(pattern = Pattern.Uniform(Color(1, 0.8, 0.1), idTf), diffuse = 0.7, specular = 0.3)))
  } yield World(PointLight(lightPosition, Color.white), List(s1, s2, s3, floorS, rightWallS, leftWallS))

  val program = for {
    w      <- world
      canvas <- RaytracingProgram.drawOnCanvas(w, cameraFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
      _      <- CanvasSerializer.>.serialize(canvas, 255)
  } yield ()
}

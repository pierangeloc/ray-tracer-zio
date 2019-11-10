package io.tuliplogic.raytracer.ops.programs

import java.nio.file.Paths

import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.data.{Color, Material, World}
import io.tuliplogic.raytracer.ops.model.data.Scene.{PointLight, Shape, Sphere}
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio._
import java.nio.file.Path

object SimpleWorld extends App{
  val cameraFrom = Pt(2, 2, -18)
  val cameraTo = Pt.origin
  val cameraUp = Vec.uy
  val hRes = 640
  val vRes = 480
  val canvasFile    = "ppm/simple-world-" + System.currentTimeMillis + ".ppm"

  val world = for {
    defaultMat <- Material.default
    s1         <- Sphere.make(Pt.origin, 3, defaultMat)
    s2         <- Sphere.make(Pt(0, 0, -5), 0.3, defaultMat)
    light      <- UIO(PointLight(Pt(0, 0, -15), Color.white))
  } yield World(light, List[Shape](s1, s2))

  val program = for {
    w      <- world
    canvas <- RaytracingProgram.drawOnCanvas(w, cameraFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
    _      <- CanvasSerializer.>.render(canvas, 255)
  } yield ()

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    program
      .provide {
        new CanvasSerializer.PPMCanvasSerializer with VerySimpleModules {
          override def path: Path = Paths.get(canvasFile)
        }
      }
      .foldM(err => console.putStrLn(s"Execution failed with: ${err.getStackTraceString}").as(1), _ => UIO.succeed(0))

}

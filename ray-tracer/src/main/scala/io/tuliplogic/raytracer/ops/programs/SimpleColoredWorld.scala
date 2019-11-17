package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.RayTracerError
import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.model.data.Scene.{PointLight, Shape, Sphere}
import io.tuliplogic.raytracer.ops.model.data.{Color, Material, Pattern, World}
import io.tuliplogic.raytracer.ops.model.modules.RasteringModule
import io.tuliplogic.raytracer.ops.rendering.CanvasSerializer
import zio._

object SimpleColoredWorld extends App{
  val cameraFrom = Pt(2, 2, -18)
  val cameraTo = Pt.origin
  val cameraUp = Vec.uy
  val hRes = 640
  val vRes = 480
  val canvasFile    = "ppm/simple-colored-world-" + System.currentTimeMillis

  val world = for {
    defaultMat <- Material.default
    idTf       <- ATModule.>.id
    s1         <- Sphere.make(Pt.origin, 3, defaultMat.copy(pattern = Pattern.Uniform(Color(255 / 256.0, 234 / 256.0, 0 / 256.0), idTf), ambient = 0.3))
    s2         <- Sphere.make(Pt(0, 0, -5), 0.3, defaultMat.copy(pattern = Pattern.Uniform(Color(13 / 256.0, 107 / 256.0, 178 / 256.0), idTf), ambient = 0.3))
    light      <- UIO(PointLight(Pt(0, 0, -15), Color.white))
  } yield World(light, List[Shape](s1, s2))

  def program(viewFrom: Pt): ZIO[CanvasSerializer with RasteringModule with ATModule, RayTracerError, Unit] = for {
    w      <- world
    canvas <- RaytracingProgram.drawOnCanvas(w, viewFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
    _      <- CanvasSerializer.>.serialize(canvas, 255)
  } yield ()

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    ZIO.traverse(-18 to -6)(z => program(Pt(2, 2, z))
      .provide {
        new CanvasSerializer.PPMCanvasSerializer with VerySimpleModulesColored {
          override def path: Path = Paths.get(s"$canvasFile-$z.ppm")
        }
      }
    ).foldM(err =>
      console.putStrLn(s"Execution failed with: $err").as(1),
      _ => UIO.succeed(0)
    )

}

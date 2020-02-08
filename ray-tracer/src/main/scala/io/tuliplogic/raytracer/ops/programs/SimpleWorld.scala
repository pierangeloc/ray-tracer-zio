package io.tuliplogic.raytracer.ops.programs

import java.nio.file.{Path, Paths}

import io.tuliplogic.raytracer.commons.errors.{ATError, RayTracerError}
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.geometry.affine.aTModule
import io.tuliplogic.raytracer.geometry.affine.aTModule.ATModule
import io.tuliplogic.raytracer.geometry.matrix.matrixModule
import io.tuliplogic.raytracer.geometry.matrix.matrixModule.MatrixModule
import io.tuliplogic.raytracer.ops.model.data.Scene.{PointLight, Shape, Sphere}
import io.tuliplogic.raytracer.ops.model.data.rayModule.RayModule
import io.tuliplogic.raytracer.ops.model.data.{Color, Material, World, rayModule}
import io.tuliplogic.raytracer.ops.model.modules.cameraModule.CameraModule
import io.tuliplogic.raytracer.ops.model.modules.lightReflectionModule.LightReflectionModule
import io.tuliplogic.raytracer.ops.model.modules.phongReflectionModule.PhongReflectionModule
import io.tuliplogic.raytracer.ops.model.modules.{cameraModule, lightDiffusionModule, lightReflectionModule, normalReflectModule, phongReflectionModule, rasteringModule, worldHitCompsModule, worldModule, worldReflectionModule, worldRefractionModule, worldTopologyModule}
import io.tuliplogic.raytracer.ops.model.modules.rasteringModule.RasteringModule
import io.tuliplogic.raytracer.ops.model.modules.worldHitCompsModule.WorldHitCompsModule
import io.tuliplogic.raytracer.ops.model.modules.worldModule.WorldModule
import io.tuliplogic.raytracer.ops.model.modules.worldTopologyModule.WorldTopologyModule
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer
import io.tuliplogic.raytracer.ops.rendering.canvasSerializer.CanvasSerializer
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.scheduler.Scheduler

object SimpleWorld extends App{
  val cameraFrom = Pt(2, 2, -18)
  val cameraTo = Pt.origin
  val cameraUp = Vec.uy
  val hRes = 640
  val vRes = 480
  val canvasFile    = "ppm/simple-world-" + System.currentTimeMillis
  type ULayer[A, B <: Has[_]] = ZLayer[A, Nothing, B]

  val world: ZIO[ATModule, ATError, World] = for {
    defaultMat <- Material.default
    s1         <- Sphere.make(Pt.origin, 3, defaultMat)
    s2         <- Sphere.make(Pt(0, 0, -5), 0.3, defaultMat)
    light      <- UIO(PointLight(Pt(0, 0, -15), Color.white))
  } yield World(light, List[Shape](s1, s2))

  def program(viewFrom: Pt, path: Path): ZIO[CanvasSerializer with RasteringModule with ATModule, RayTracerError, Unit] = for {
    w      <- world
    canvas <- RaytracingProgram.drawOnCanvas(w, viewFrom, cameraTo, cameraUp, math.Pi / 3, hRes, vRes)
    _      <- canvasSerializer.serializeToFile(canvas, 255, path)
  } yield ()

  import layers._

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    ZIO.traverse(-18 to -6)(z => program(Pt(2, 2, z), Paths.get(s"$canvasFile-$z.ppm"))
      .provideLayer(cSerializerM ++ (atM >>> rasteringM) ++ atM)
    ).timed.foldM(err =>
    console.putStrLn(s"Execution failed with: $err").as(1),
    { case (duration, _) => console.putStrLn(s"rendering took ${duration.toMillis} ms") *> UIO.succeed(0) }
  )

}

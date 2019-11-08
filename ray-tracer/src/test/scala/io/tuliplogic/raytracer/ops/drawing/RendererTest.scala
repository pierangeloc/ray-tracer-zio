package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.{Pt, Vec}
import io.tuliplogic.raytracer.ops.OpsTestUtils
import io.tuliplogic.raytracer.ops.model.{CameraModule, Color, LightDiffusionModule, LightReflectionModule, NormalReflectModule, PhongReflectionModule, RayModule, WorldHitCompsModule, WorldModule, WorldReflectionModule, WorldRefractionModule, WorldTopologyModule}
import zio.stream.Sink
import zio.{DefaultRuntime, Task, UIO}
import org.scalatest.WordSpec
import org.scalatest.Matchers._

class RendererTest extends WordSpec with DefaultRuntime with OpsTestUtils {

  val env = new WorldModule.Live with WorldTopologyModule.Live with WorldHitCompsModule.Live
    with ATModule.Live with MatrixModule.BreezeMatrixModule with WorldReflectionModule.Live
    with LightReflectionModule.Live with LightDiffusionModule.Live
    with WorldRefractionModule.Live with PhongReflectionModule.Live with NormalReflectModule.Live with RayModule.Live
    with CameraModule.Live
  "renderer" should {
    "produce the expected color for the pixel" in {
      unsafeRun {
        (for {
          w    <- WorldTest.defaultWorld
          from <- UIO(Pt(0, 0, -5))
          to   <- UIO(Pt.origin)
          up   <- UIO(Vec(0, 1, 0))
          camera <- Camera.make(from, to, up, math.Pi / 2, 11, 11)
          pixels <- Renderer
            .render(camera, w)
            .flattenChunks
            .collect {
              case (5, 5, c) => c
            }
            .run(Sink.collectAllN[Color](1))
          _ <- Task.effectTotal { pixels.head should ===(Color(0.38066, 0.47583, 0.2855)) }
        } yield pixels)
          .provide(env)
      }
    }
  }
}

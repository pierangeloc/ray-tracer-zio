package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.affine.ATModule
import io.tuliplogic.raytracer.geometry.affine.PointVec.Pt
import io.tuliplogic.raytracer.geometry.matrix.MatrixModule
import io.tuliplogic.raytracer.ops.model.Color
import org.scalatest.WordSpec
import org.scalatest.Matchers._
import zio.DefaultRuntime

class PatternTest extends WordSpec with DefaultRuntime {

  val env = new ATModule.Live with MatrixModule.BreezeMatrixModule
  "Striped pattern" should {
    val p = unsafeRun(ATModule.>.id.map(Pattern.Striped(Color.white, Color.black, _)).provide(env))
    "be constant in y" in {
      (0 to 100).toList.map(Pt(0, _, 0)).forall(p(_) == Color.white) shouldEqual true
    }

    "be constant in z" in {
      (0 to 100).toList.map(Pt(0, 0, _)).forall(p(_) == Color.white) shouldEqual true
    }

    "alternate on integer x" in {
      (0 to 100).toList.map(x => Pt(x + 0.1, 0, 0)).map(p) shouldEqual (0 to 100).toList.map(n => if (n % 2 == 0) Color.white else Color.black)
    }
  }

  "Gradient pattern" should {
    val p = unsafeRun(ATModule.>.id.map(Pattern.GradientX(Color.white, Color.black, _)).provide(env))
    "determine correctly the colors" in {
      p(Pt.origin) shouldEqual Color.white
      p(Pt(0.25, 0, 0)) shouldEqual Color(0.75, 0.75, 0.75)
      p(Pt(0.5, 0, 0)) shouldEqual Color(0.5, 0.5, 0.5)
      p(Pt(0.75, 0, 0)) shouldEqual Color(0.25, 0.25, 0.25)
    }
  }

  "Ring pattern" should {
    val p = unsafeRun(ATModule.>.id.map(Pattern.Ring(Color.white, Color.black, _)).provide(env))
    "determine correctly the colors" in {
      p(Pt.origin) shouldEqual Color.white
      p(Pt(1, 0, 0)) shouldEqual Color.black
      p(Pt(0, 0, 1)) shouldEqual Color.black
      p(Pt(math.sqrt(2) / 2, 0, math.sqrt(2) / 2)) shouldEqual Color.black
    }
  }

  "Checker pattern" should {
    val p = unsafeRun(ATModule.>.id.map(Pattern.Checker(Color.white, Color.black, _)).provide(env))
    "be periodic of 2 in x" in {
      p(Pt.origin) shouldEqual Color.white
      p(Pt(0.99, 0, 0)) shouldEqual Color.white
      p(Pt(1.01, 0, 0)) shouldEqual Color.black
    }

    "be periodic of 2 in y" in {
      p(Pt.origin) shouldEqual Color.white
      p(Pt(0, 0.99, 0)) shouldEqual Color.white
      p(Pt(0, 1.01, 0)) shouldEqual Color.black
    }

    "be periodic of 2 in z" in {
      p(Pt.origin) shouldEqual Color.white
      p(Pt(0, 0, 0.99)) shouldEqual Color.white
      p(Pt(0, 0, 1.01)) shouldEqual Color.black
    }
  }
}

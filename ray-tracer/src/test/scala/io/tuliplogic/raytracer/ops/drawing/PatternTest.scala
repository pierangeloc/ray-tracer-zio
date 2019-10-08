package io.tuliplogic.raytracer.ops.drawing

import io.tuliplogic.raytracer.geometry.vectorspace.PointVec.Pt
import io.tuliplogic.raytracer.ops.model.Color
import org.scalatest.WordSpec
import org.scalatest.Matchers._

class PatternTest extends WordSpec {

  "Striped pattern" should {
    val p = Pattern.Striped(Color.white, Color.black)
    "be constant in y" in {
      (0 to 100).toList.map(Pt(0, _, 0)).forall(p(_) == Color.white) shouldEqual true
    }

    "be constant in z" in {
      (0 to 100).toList.map(Pt(0, 0, _)).forall(p(_) == Color.white) shouldEqual true
    }

    "alternate on integer x" in {
      (0 to 100).toList.map(x => Pt(x + 0.1, 0, 0)).map(p) shouldEqual (0 to 100).toList.map(n => if(n % 2 == 0) Color.white else Color.black)
    }
  }
}

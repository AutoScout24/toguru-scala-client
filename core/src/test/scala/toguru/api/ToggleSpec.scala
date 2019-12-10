package toguru.api

import org.scalatest.{FeatureSpec, MustMatchers}
import toguru.test.TestActivations

class ToggleSpec extends FeatureSpec with MustMatchers {

  val emptyToggleInfo = TogglingInfo(ClientInfo(), new TestActivations.Impl()())

  feature("Toggle creation") {
    scenario("default condition is false") {
      val toggle1             = Toggle("toggle-1")
      implicit val toggleInfo = emptyToggleInfo

      toggle1.isOn mustBe false
      toggle1.isOff mustBe true
    }

    scenario("default condition is respected") {
      val toggle1             = Toggle("toggle-1", default = Condition.On)
      implicit val toggleInfo = emptyToggleInfo

      toggle1.isOn mustBe true
      toggle1.isOff mustBe false
    }
  }
}

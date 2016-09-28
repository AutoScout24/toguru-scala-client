package toguru.api

import org.scalatest.{FeatureSpec, ShouldMatchers}
import toguru.test.TestActivations

class ToggleSpec extends FeatureSpec with ShouldMatchers {

  val emptyToggleInfo = TogglingInfo(ClientInfo(), new TestActivations.Impl()())

  feature("Toggle creation") {
    scenario("default condition is false") {
      val toggle1 = Toggle("toggle-1")
      implicit val toggleInfo = emptyToggleInfo

      toggle1.isOn shouldBe false
      toggle1.isOff shouldBe true
    }

    scenario("default condition is respected") {
      val toggle1 = Toggle("toggle-1", default = Condition.On)
      implicit val toggleInfo = emptyToggleInfo

      toggle1.isOn shouldBe true
      toggle1.isOff shouldBe false
    }
  }
}

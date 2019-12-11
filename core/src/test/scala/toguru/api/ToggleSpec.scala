package toguru.api

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers
import toguru.test.TestActivations

class ToggleSpec extends AnyFeatureSpec with Matchers {

  val emptyToggleInfo = TogglingInfo(ClientInfo(), new TestActivations.Impl()())

  Feature("Toggle creation") {
    Scenario("default condition is false") {
      val toggle1             = Toggle("toggle-1")
      implicit val toggleInfo = emptyToggleInfo

      toggle1.isOn mustBe false
      toggle1.isOff mustBe true
    }

    Scenario("default condition is respected") {
      val toggle1             = Toggle("toggle-1", default = Condition.On)
      implicit val toggleInfo = emptyToggleInfo

      toggle1.isOn mustBe true
      toggle1.isOff mustBe false
    }
  }
}

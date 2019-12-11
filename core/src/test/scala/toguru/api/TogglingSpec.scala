package toguru.api

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers
import toguru.helpers.ClientInfoHelper
import toguru.helpers.ClientInfoHelper._
import toguru.test.TestActivations

class TogglingSpec extends AnyFeatureSpec with Matchers {

  Feature("Can change toggle state") {
    Scenario("toggle state in toggle info is applied") {
      val toggle1    = Toggle("toggle-1", default = Condition.Off)
      val activation = new TestActivations.Impl(toggle1 -> Condition.On)()

      implicit val info = TogglingInfo(ClientInfo(), activation)

      toggle1.isOn mustBe true
    }

    Scenario("default toggle state is applied") {
      val toggle1    = Toggle("toggle-1", default = Condition.On)
      val activation = new TestActivations.Impl()()

      implicit val info = TogglingInfo(ClientInfo(), activation)

      toggle1.isOn mustBe true
    }

    Scenario("toggle state can be forced by client info") {
      val toggle1    = Toggle("toggle-1", default = Condition.Off)
      val activation = new TestActivations.Impl(toggle1 -> Condition.Off)()
      val info       = ClientInfo(forcedToggle = ClientInfoHelper.forceToggleTo("toggle-1", enabled = true))

      implicit val toggleInfo = TogglingInfo(info, activation)

      toggle1.isOn mustBe true
    }

    Scenario("toggle state falls back to false if client uuid is None") {
      val toggle1    = Toggle("toggle-1", default = Condition.On)
      val activation = new TestActivations.Impl(toggle1 -> Condition.UuidRange(1 to 100))()
      val info       = ClientInfo(uuid = None)

      implicit val toggleInfo = TogglingInfo(info, activation)

      toggle1.isOn mustBe false
    }
  }

  Feature("Can build toggle strings") {
    import toguru.implicits.toggle._

    Scenario("produces 'on' when toggle state is 'off' but the client overrides it to be 'on'") {
      val toggle              = Toggle("feature1")
      val activations         = TestActivations(toggle -> Condition.Off)(toggle -> "service1")()
      val clientInfo          = ClientInfo(None, forceToggleTo("feature1", enabled = true))
      implicit val toggleInfo = TogglingInfo(clientInfo, activations)

      toggleInfo().buildString mustBe ("feature1=true")
    }

    Scenario("produces 'off' when toggle state is 'on' but the client overrides it to be 'off'") {
      val toggle                          = Toggle("feature1")
      val activations                     = TestActivations(toggle -> Condition.On)(toggle -> "service1")()
      implicit val clientInfo: ClientInfo = ClientInfo(None, forceToggleTo("feature1", enabled = false))
      implicit val toggleInfo             = TogglingInfo(clientInfo, activations)

      toggleInfo().buildString mustBe ("feature1=false")
    }

    Scenario("produces 'on' when toggle state is 'on'") {
      val toggle                          = Toggle("feature1")
      val activations                     = TestActivations(toggle -> Condition.On)(toggle -> "service1")()
      implicit val clientInfo: ClientInfo = ClientInfo()
      implicit val toggleInfo             = TogglingInfo(clientInfo, activations)

      toggleInfo().buildString mustBe ("feature1=true")
    }

  }
}

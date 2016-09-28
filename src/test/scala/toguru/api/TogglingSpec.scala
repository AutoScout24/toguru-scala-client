package toguru.api

import org.scalatest.{FeatureSpec, _}
import toguru.helpers.ClientInfoHelper
import toguru.helpers.ClientInfoHelper._
import toguru.test.TestActivations

class TogglingSpec extends FeatureSpec with ShouldMatchers {

  feature("Can change toggle state") {
    scenario("toggle state in toggle info is applied") {
      val toggle1 = Toggle("toggle-1", default = Condition.Off)
      val activation = new TestActivations.Impl(toggle1 -> Condition.On)()

      implicit val info = TogglingInfo(ClientInfo(), activation)

      toggle1.isOn shouldBe true
    }

    scenario("default toggle state is applied") {
      val toggle1 = Toggle("toggle-1", default = Condition.On)
      val activation = new TestActivations.Impl()()

      implicit val info = TogglingInfo(ClientInfo(), activation)

      toggle1.isOn shouldBe true
    }

    scenario("toggle state can be forced by client info") {
      val toggle1 = Toggle("toggle-1", default = Condition.Off)
      val activation = new TestActivations.Impl(toggle1 -> Condition.Off)()
      val info = ClientInfo(forcedToggle = ClientInfoHelper.forceToggleTo("toggle-1", enabled = true))

      implicit val toggleInfo = TogglingInfo(info, activation)

      toggle1.isOn shouldBe true
    }
  }

  feature("Can build toggle strings") {

    scenario("produces 'on' when toggle state is 'off' but the client overrides it to be 'on'") {
      val toggle = Toggle("feature1")
      val activations = TestActivations(toggle -> Condition.Off)(toggle -> "service1")()
      val clientInfo = ClientInfo(None, None, None, forceToggleTo("feature1", enabled = true))
      val toggleInfo = TogglingInfo(clientInfo, activations)

      toggleInfo.toggleStringForService("service1") should be("feature1=true")
    }

    scenario("produces 'off' when toggle state is 'on' but the client overrides it to be 'off'") {
      val toggle = Toggle("feature1")
      val activations = TestActivations(toggle -> Condition.On)(toggle -> "service1")()
      implicit val clientInfo: ClientInfo = ClientInfo(None, None, None, forceToggleTo("feature1", enabled = false))
      val toggleInfo = TogglingInfo(clientInfo, activations)

      toggleInfo.toggleStringForService("service1") should be("feature1=false")
    }

    scenario("produces 'on' when toggle state is 'on'") {
      val toggle = Toggle("feature1")
      val activations = TestActivations(toggle -> Condition.On)(toggle -> "service1")()
      implicit val clientInfo: ClientInfo = ClientInfo()
      val toggleInfo = TogglingInfo(clientInfo, activations)

      toggleInfo.toggleStringForService("service1") should be("feature1=true")
    }

  }
}
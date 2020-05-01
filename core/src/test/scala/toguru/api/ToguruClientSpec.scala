package toguru.api

import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers
import toguru.api.Activations.Provider

class ToguruClientSpec extends AnyFeatureSpec with Matchers with IdiomaticMockito {

  val mockClientInfo = ClientInfo()

  val mockClientProvider: ClientInfo.Provider[String] = _ => mockClientInfo

  def activationProvider(activations: Activations = DefaultActivations, health: Boolean): Activations.Provider =
    new Provider {
      def healthy() = health
      def apply()   = activations
      def close()   = ()
    }

  def toguruClient(
      clientProvider: ClientInfo.Provider[String] = mockClientProvider,
      activations: Activations.Provider
  ) =
    new ToguruClient(clientProvider, activations)

  Feature("health check") {
    Scenario("activations provider is healthy") {
      val client = toguruClient(activations = activationProvider(health = true))

      client.healthy() mustBe true
    }

    Scenario("activations provider is unhealthy") {
      val client = toguruClient(activations = activationProvider(health = false))

      client.healthy() mustBe false
    }
  }

  Feature("client info provider") {
    Scenario("client info requested") {
      val myActivations      = mock[Activations]
      val myInfo: ClientInfo = ClientInfo().withAttribute("user", "me")
      val client = toguruClient(
        clientProvider = _ => myInfo,
        activations = activationProvider(health = true, activations = myActivations)
      )

      val toggling = client.apply("client")

      toggling.activations mustBe myActivations
      toggling.client mustBe myInfo
    }
  }
}

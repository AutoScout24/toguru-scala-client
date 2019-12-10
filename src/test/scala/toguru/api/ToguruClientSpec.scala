package toguru.api

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FeatureSpec, _}
import toguru.api.Activations.Provider

class ToguruClientSpec extends FeatureSpec with MustMatchers with MockitoSugar {

  val mockClientInfo = ClientInfo()

  val mockClientProvider: ClientInfo.Provider[String] = _ => mockClientInfo

  def activationProvider(activations: Activations = DefaultActivations, health: Boolean): Activations.Provider =
    new Provider {
      def healthy() = health
      def apply()   = activations
    }

  def toguruClient(
      clientProvider: ClientInfo.Provider[String] = mockClientProvider,
      activations: Activations.Provider
  ) =
    new ToguruClient(clientProvider, activations)

  feature("health check") {
    scenario("activations provider is healthy") {
      val client = toguruClient(activations = activationProvider(health = true))

      client.healthy() mustBe true
    }

    scenario("activations provider is unhealthy") {
      val client = toguruClient(activations = activationProvider(health = false))

      client.healthy() mustBe false
    }
  }

  feature("client info provider") {
    scenario("client info requested") {
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

package toguru.impl

import java.util.UUID
import java.util.concurrent.Executors

import org.scalatest.{OptionValues, ShouldMatchers, WordSpec}
import toguru.api.{ClientInfo, Condition, DefaultActivations, Toggle}
import toguru.impl.RemoteActivationsProvider.TogglePoller

class RemoteActivationsProviderSpec extends WordSpec with OptionValues with ShouldMatchers {

  val toggleOne = Toggle("toggle-one")
  val toggleTwo = Toggle("toggle-two")
  val executor = Executors.newSingleThreadScheduledExecutor()

  def createProvider(poller: TogglePoller): RemoteActivationsProvider =
    new RemoteActivationsProvider(poller, executor).close()

  "Fetching features from toggle endpoint" should {
    "succeed if toggle endpoint is available" in {
      val response =
        """
          |[
          |  { "id": "toggle-one", "tags": {"services": "toguru"}},
          |  { "id": "toggle-two", "tags": {"team": "Shared Services"}, "rolloutPercentage": 20}
          |]
        """.stripMargin

      val poller: TogglePoller = () => (200, response)

      val provider = createProvider(poller)

      val toggles = provider.fetchToggleStates().value
      provider.update()
      val activations = provider.apply()

      val toggleStateOne = toggles.collectFirst { case t if t.id == toggleOne.id => t }.value
      val toggleStateTwo = toggles.collectFirst { case t if t.id == toggleTwo.id => t }.value

      toggleStateOne.id shouldBe "toggle-one"
      toggleStateOne.tags shouldBe Map("services" -> "toguru")
      toggleStateOne.rolloutPercentage shouldBe None

      toggleStateTwo.id shouldBe "toggle-two"
      toggleStateTwo.tags shouldBe Map("team" -> "Shared Services")
      toggleStateTwo.rolloutPercentage shouldBe Some(20)

      activations.apply(toggleOne) shouldBe Condition.Off
    }

    "fail if toggle endpoint returns 500" in {
      val response = ""

      val poller: TogglePoller = () => (500, response)
      val provider = createProvider(poller)

      provider.fetchToggleStates() shouldBe None

      provider.update()
      provider.apply() shouldBe DefaultActivations
    }

    "fail if toggle endpoint returns malformed json" in {
      val response = "ok"

      val poller: TogglePoller = () => (200, response)
      val provider = createProvider(poller)

      provider.fetchToggleStates() shouldBe None

      provider.update()
      provider.apply() shouldBe DefaultActivations
    }

    "fail if poller throws exception" in {
      val poller: TogglePoller = () => throw new RuntimeException("boom")
      val provider = createProvider(poller)

      provider.fetchToggleStates() shouldBe None
    }
  }

  "Creating activation provider" should {
    "succeed with a valid url" in {
      val url = "localhost:9000/togglestate"

      val provider = RemoteActivationsProvider(url)
      provider.close()
    }
  }
}

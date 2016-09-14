package toguru.impl

import java.util.UUID

import org.scalatest.{FeatureSpec, MustMatchers, OptionValues}
import toguru.ClientInfoImpl
import toguru.api.FeatureImpl
import toguru.impl.PollingRegistryUpdater.{EventPublisher, TogglePoller}

class PollingRegistryUpdaterSpec extends FeatureSpec with OptionValues with MustMatchers {

  feature("Fetch features from toggle endpoint") {
    scenario("toggle endpoint is available") {
      val response =
        """
          |[
          |  { "id": "toggle-one", "tags": {"services": "toguru"}},
          |  { "id": "toggle-two", "tags": {"team": "Shared Services"}, "rolloutPercentage": 20}
          |]
        """.stripMargin

      val poller: TogglePoller = () => (200, response)
      val publisher: EventPublisher = (name, fields) => {}

      val update = new PollingRegistryUpdater(poller, publisher).apply()

      val (registry, _) = update.value

      val toggleOne = registry.feature("toggle-one").value.asInstanceOf[FeatureImpl].desc
      val toggleTwo = registry.feature("toggle-two").value.asInstanceOf[FeatureImpl].desc
      val clientInfo = ClientInfoImpl(uuid = Some(UUID.fromString("00000000-0000-0000-0000-000000000000")))

      toggleOne.name must be("toggle-one")
      toggleOne.tags must be(Some(Set("toguru")))
      toggleOne.activation must be(Set(AlwaysOffCondition))
      toggleOne.services must be(Some(Set("toguru")))

      toggleTwo.name must be("toggle-two")
      toggleTwo.tags must be(Some(Set("Shared Services")))

      val condition = toggleTwo.activation.to[Seq].head
      condition must be(a[UuidDistributionCondition])
      condition.asInstanceOf[UuidDistributionCondition].ranges mustBe Seq(1 to 20)

      registry.allFeatures.map(_._1) must be(Set("toggle-one", "toggle-two"))

      registry.featureStringForService("toguru")(clientInfo) must be("toggle-one=false")
    }

    scenario("toggle endpoint returns 500") {
      val response = ""

      val poller: TogglePoller = () => (500, response)
      val publisher: EventPublisher = (name, fields) => {}

      val update = new PollingRegistryUpdater(poller, publisher).apply()

      update must be(None)
    }

    scenario("toggle endpoint returns malformed json") {
      val response = "ok"

      val poller: TogglePoller = () => (200, response)
      val publisher: EventPublisher = (name, fields) => {}

      val update = new PollingRegistryUpdater(poller, publisher).apply()

      update must be(None)
    }

    scenario("poller throws exception") {
      val poller: TogglePoller = () => throw new RuntimeException("boom")
      val publisher: EventPublisher = (name, fields) => {}

      val update = new PollingRegistryUpdater(poller, publisher).apply()

      update must be(None)
    }
  }

  feature("can be created from config") {
    val publisher: EventPublisher = (name, fields) => {}
    val url = "localhost:9000/togglestate"

    val updater = PollingRegistryUpdater(url, publisher)
  }
}

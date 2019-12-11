package toguru.api

import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers
import toguru.impl.RemoteActivationsProvider

class ActivationsSpec extends AnyFeatureSpec with Matchers with IdiomaticMockito {

  Feature("Remote activations provider") {
    Scenario("can be created") {
      val provider = Activations.fromEndpoint("http://localhost:9000/togglestates")

      provider mustBe a[RemoteActivationsProvider]
      provider.asInstanceOf[RemoteActivationsProvider].close()
    }
  }

  Feature("Default activations") {
    Scenario("return toggle's default activation") {
      val condition = mock[Condition]
      val toggle    = Toggle("toggle-1", condition)

      DefaultActivations.apply(toggle) mustBe condition
    }

    Scenario("return empty service toggles") {
      DefaultActivations.togglesFor("my-service") mustBe Map.empty
    }

    Scenario("return None for sequenceNo") {
      DefaultActivations.stateSequenceNo mustBe None
    }
  }
}

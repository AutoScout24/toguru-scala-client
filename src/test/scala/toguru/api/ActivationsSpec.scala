package toguru.api

import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FeatureSpec, MustMatchers}
import toguru.impl.RemoteActivationsProvider

class ActivationsSpec extends FeatureSpec with MustMatchers with MockitoSugar {

  feature("Remote activations provider") {
    scenario("can be created") {
      val provider = Activations.fromEndpoint("http://localhost:9000/togglestates")

      provider mustBe a[RemoteActivationsProvider]
      provider.asInstanceOf[RemoteActivationsProvider].close()
    }
  }

  feature("Default activations") {
    scenario("return toggle's default activation") {
      val condition = mock[Condition]
      val toggle    = Toggle("toggle-1", condition)

      DefaultActivations.apply(toggle) mustBe condition
    }

    scenario("return empty service toggles") {
      DefaultActivations.togglesFor("my-service") mustBe Map.empty
    }

    scenario("return None for sequenceNo") {
      DefaultActivations.stateSequenceNo mustBe None
    }
  }
}

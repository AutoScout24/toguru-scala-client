package toguru.api

import org.scalatest.mock.MockitoSugar
import org.scalatest.{FeatureSpec, ShouldMatchers}
import toguru.impl.RemoteActivationsProvider

class ActivationsSpec extends FeatureSpec with ShouldMatchers with MockitoSugar {

  feature("Remote activations provider") {
    scenario("can be created") {
      val provider = Activations.fromEndpoint("http://localhost:9000/togglestates")

      provider shouldBe a[RemoteActivationsProvider]
      provider.asInstanceOf[RemoteActivationsProvider].close()
    }
  }

  feature("Default activations") {
    scenario("return toggle's default activation") {
      val condition = mock[Condition]
      val toggle = Toggle("toggle-1", condition)

      DefaultActivations.apply(toggle) shouldBe condition
    }

    scenario("return empty service toggles") {
      DefaultActivations.togglesFor("my-service") shouldBe Map.empty
    }

    scenario("return None for sequenceNo") {
      DefaultActivations.stateSequenceNo shouldBe None
    }
  }
}

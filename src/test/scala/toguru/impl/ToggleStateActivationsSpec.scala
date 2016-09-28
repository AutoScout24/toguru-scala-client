package toguru.impl

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ShouldMatchers, WordSpec}
import toguru.api.{Condition, Toggle}

class ToggleStateActivationsSpec extends WordSpec with ShouldMatchers with MockitoSugar {

  val toggle1 = Toggle("toggle-1")

  val activations = new ToggleStateActivations(List(
    ToggleState(toggle1.id, Map("services" -> "toguru"), Some(30)),
    ToggleState("toggle-2", Map.empty, Some(100))
  ))

  "ToggleStateActivations" should {
    "return activations from toggle state" in {
      val condition = activations.apply(toggle1)

      condition shouldBe a[UuidDistributionCondition]

      val uuidCondition = condition.asInstanceOf[UuidDistributionCondition]

      uuidCondition.ranges shouldBe List(1 to 30)
    }

    "return toggle conditions for services" in {
      val toggles = activations.togglesFor("toguru")

      toggles should have size 1
      toggles.keySet shouldBe Set(toggle1.id)

      val condition = toggles(toggle1.id)

      condition shouldBe a[UuidDistributionCondition]

      val uuidCondition = condition.asInstanceOf[UuidDistributionCondition]

      uuidCondition.ranges shouldBe List(1 to 30)
    }

    "return toggle default conditions if toggle is unknown" in {
      val condition = mock[Condition]
      val toggle = Toggle("toggle-3", condition)

      activations.apply(toggle) shouldBe condition
    }

  }
}

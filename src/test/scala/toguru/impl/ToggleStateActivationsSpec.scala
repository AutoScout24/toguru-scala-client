package toguru.impl

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ShouldMatchers, WordSpec}
import toguru.api.{Condition, Toggle}

class ToggleStateActivationsSpec extends WordSpec with ShouldMatchers with MockitoSugar {

  def rollout(r: Int) = Some(Rollout(r))

  val toggle1 = Toggle("toggle-1")
  val toggle3 = Toggle("toggle-4")

  val toggles = List(
    ToggleState(toggle1.id, Map("services" -> "toguru"), rollout(30)),
    ToggleState("toggle-2", Map.empty, rollout(100)),
    ToggleState("toggle-4", Map.empty, None, Map("culture" -> Seq("DE", "de-DE"), "version" -> Seq("1", "2"))))

  val activations = new ToggleStateActivations(ToggleStates(Some(10), toggles))

  "ToggleStateActivations" should {
    "return activations from toggle state" in {
      val condition = activations.apply(toggle1)

      condition shouldBe a[UuidDistributionCondition]

      val uuidCondition = condition.asInstanceOf[UuidDistributionCondition]

      uuidCondition.ranges shouldBe List(1 to 30)
    }

    "create attribute activations from toggle state" in {
      val condition = activations.apply(toggle3)

      condition shouldBe All(Set(Attribute("culture", Seq("DE", "de-DE")), Attribute("version", Seq("1", "2"))))
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

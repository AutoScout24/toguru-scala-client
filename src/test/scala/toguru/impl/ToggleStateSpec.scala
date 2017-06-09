package toguru.impl

import org.scalatest.mock.MockitoSugar
import org.scalatest.{ShouldMatchers, WordSpec}
import toguru.api.{Condition, Toggle}

class ToggleStateSpec extends WordSpec with ShouldMatchers with MockitoSugar {

  def activation(rollout: Option[Rollout] = None, attrs: Map[String, Seq[String]] = Map.empty) =
    Seq(ToggleActivation(rollout, attrs))

  def rollout(r: Int) = Some(Rollout(r))

  val toggles = List(
    ToggleState("toggle1", Map("services" -> "toguru"), activation(rollout(30))),
    ToggleState("toggle-2", Map.empty[String, String], activation(rollout(100))),
    ToggleState("toggle-4", Map.empty[String, String], activation(attrs = Map("culture" -> Seq("DE", "de-DE"), "version" -> Seq("1", "2")))))


  "ToggleState.apply" should {

    "transform activations into conditions" in {
      val condition = toggles(0).condition

      condition shouldBe a[UuidDistributionCondition]

      val uuidCondition = condition.asInstanceOf[UuidDistributionCondition]

      uuidCondition.ranges shouldBe List(1 to 30)
    }

    "transform activations attributes to conditions" in {
      val condition = toggles(2).condition

      condition shouldBe All(Set(Attribute("culture", Seq("DE", "de-DE")), Attribute("version", Seq("1", "2"))))
    }

  }

  "ToggleState.activations" should {

    val activations = new ToggleStateActivations(ToggleStates(Some(10), toggles))

    "return toggle conditions for services" in {
      val toguruToggles = activations.togglesFor("toguru")

      toguruToggles should have size 1
      toguruToggles.keySet shouldBe Set("toggle1")

      val condition = toguruToggles("toggle1")

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

package toguru.impl

import java.util.UUID

import org.scalatest.{MustMatchers, WordSpec}
import toguru.api.ClientInfo

class ConditionsSpec extends WordSpec with MustMatchers {

  "Attribute conditions" should {
    "apply correctly to de-DE culture" in {
      val clientInfo = ClientInfo(attributes = Map("culture" -> "de-DE"))

      Attribute("culture", Seq("de-DE")).applies(clientInfo) mustBe true
    }

    "apply if one value matches" in {
      val clientInfo = ClientInfo(attributes = Map("culture" -> "de-DE"))

      Attribute("culture", Seq("de-DE", "de-AT")).applies(clientInfo) mustBe true
    }
  }

  "All conditions" should {
    val myAllCondition = All(Set(Attribute("one", Seq("one")), Attribute("two", Seq("two"))))

    "return true if no condition is given" in {
      val clientInfo = ClientInfo()
      All(Set.empty).applies(clientInfo) mustBe true
    }

    "Evaluate to true if all conditions are met" in {
      val clientInfo = ClientInfo(attributes = Map("one" -> "one", "two" -> "two"))
      myAllCondition.applies(clientInfo) mustBe true
    }

    "Evaluate to false if one condition is unmet" in {
      val clientInfo = ClientInfo(attributes = Map("one" -> "one"))
      myAllCondition.applies(clientInfo) mustBe false
    }
  }

  "Uuid distribution conditions" should {
    val uuidWithDefaultProjectionToFive = UUID.fromString("56ed135b-a474-41d4-bde1-bc5e1e8bf910")

    "apply to true if uuid is projected into the given range" in {
      val clientInfo = ClientInfo(uuid = Some(uuidWithDefaultProjectionToFive))
      UuidDistributionCondition(3 to 5).applies(clientInfo) mustBe true
    }

    "apply to false if uuid is projected outside the given range" in {
      val clientInfo = ClientInfo(uuid = Some(uuidWithDefaultProjectionToFive))
      UuidDistributionCondition(10 to 11).applies(clientInfo) mustBe false
    }

    "apply to false if uuid is none and rollout is 100%" in {
      val clientInfo = ClientInfo(uuid = None)
      UuidDistributionCondition(1 to 100).applies(clientInfo) mustBe false
    }

    "apply to false if uuid is none and rollout is 1%" in {
      val clientInfo = ClientInfo(uuid = None)
      UuidDistributionCondition(1 to 1).applies(clientInfo) mustBe false
    }

    "throw an exception if the range is not between 1 and 100" in {
      intercept[IllegalArgumentException] {
        UuidDistributionCondition(100 to 101)
      }
    }
  }

  "Default uuid to int projection" should {
    val uuid = UUID.randomUUID()

    s"project random uuid ($uuid) between 1 and 100" in {
      val projected = UuidDistributionCondition.defaultUuidToIntProjection(uuid)
      projected mustBe >(0)
      projected mustBe <=(100)
    }
  }
}

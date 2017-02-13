package toguru.api

import org.scalatest.{FeatureSpec, _}

class ConditionSpec extends FeatureSpec with ShouldMatchers {

  feature("Off Condition") {
    scenario("can be created by API") {
      Condition.Off shouldBe a[Condition]
    }
  }

  feature("On Condition") {
    scenario("can be created") {
      Condition.On shouldBe a[Condition]
    }
  }

  feature("Rollout Range Condition") {
    scenario("can be created") {
      Condition.UuidRange(1 to 20) shouldBe a[Condition]
    }
  }

  feature("Attribute Condition") {
    scenario("can be created") {
      Condition.Attribute("myAttribute", "one", "two", "three") shouldBe a[Condition]
    }
  }

  feature("Combined Condition") {
    scenario("can be created") {
      import Condition._
      Condition(
        UuidRange(1 to 20),
        Attribute("myAttribute", "one", "two", "three")) shouldBe a[Condition]
    }

    scenario("when created from one condition yields the given condition") {
      Condition(Condition.Off) shouldBe Condition.Off
    }

    scenario("when created from empty conditions yields Condition 'On'") {
      Condition() shouldBe Condition.On
    }
  }
}

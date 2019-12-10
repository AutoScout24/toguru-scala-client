package toguru.api

import org.scalatest.{FeatureSpec, _}

class ConditionSpec extends FeatureSpec with MustMatchers {

  feature("Off Condition") {
    scenario("can be created by API") {
      Condition.Off mustBe a[Condition]
    }
  }

  feature("On Condition") {
    scenario("can be created") {
      Condition.On mustBe a[Condition]
    }
  }

  feature("Rollout Range Condition") {
    scenario("can be created") {
      Condition.UuidRange(1 to 20) mustBe a[Condition]
    }
  }

  feature("Attribute Condition") {
    scenario("can be created") {
      Condition.Attribute("myAttribute", "one", "two", "three") mustBe a[Condition]
    }
  }

  feature("Combined Condition") {
    scenario("can be created") {
      import Condition._
      Condition(UuidRange(1 to 20), Attribute("myAttribute", "one", "two", "three")) mustBe a[Condition]
    }

    scenario("when created from one condition yields the given condition") {
      Condition(Condition.Off) mustBe Condition.Off
    }

    scenario("when created from empty conditions yields Condition 'On'") {
      Condition() mustBe Condition.On
    }
  }
}

package toguru.api

import org.scalatest.featurespec.AnyFeatureSpec
import org.scalatest.matchers.must.Matchers

class ConditionSpec extends AnyFeatureSpec with Matchers {

  Feature("Off Condition") {
    Scenario("can be created by API") {
      Condition.Off mustBe a[Condition]
    }
  }

  Feature("On Condition") {
    Scenario("can be created") {
      Condition.On mustBe a[Condition]
    }
  }

  Feature("Rollout Range Condition") {
    Scenario("can be created") {
      Condition.UuidRange(1 to 20) mustBe a[Condition]
    }
  }

  Feature("Attribute Condition") {
    Scenario("can be created") {
      Condition.Attribute("myAttribute", "one", "two", "three") mustBe a[Condition]
    }
  }

  Feature("Combined Condition") {
    Scenario("can be created") {
      import Condition._
      Condition(UuidRange(1 to 20), Attribute("myAttribute", "one", "two", "three")) mustBe a[Condition]
    }

    Scenario("when created from one condition yields the given condition") {
      Condition(Condition.Off) mustBe Condition.Off
    }

    Scenario("when created from empty conditions yields Condition 'On'") {
      Condition() mustBe Condition.On
    }
  }
}

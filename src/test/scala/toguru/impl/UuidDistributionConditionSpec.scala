package toguru.impl

import java.util.UUID

import toguru.ClientInfo
import org.scalatest.{FeatureSpec, MustMatchers}

class UuidDistributionConditionSpec extends FeatureSpec with MustMatchers {

  feature("Invalid ranges") {
    scenario("Lower boundary is too low") {

      intercept[IllegalArgumentException] {
        UuidDistributionCondition.apply(0 to 10)
      }.getMessage must be("Range should describe a range between 1 and 100 inclusive")
    }

    scenario("Upper boundary is too high") {
      intercept[IllegalArgumentException] {
        UuidDistributionCondition.apply(90 to 101)
      }.getMessage must be("Range should describe a range between 1 and 100 inclusive")
    }
  }

  feature("Valid ranges") {
    scenario("max range") {

      1 to 1000 foreach {
        _ =>
          val uuid = UUID.randomUUID()
          val info = ClientInfo(uuid = Some(uuid))
          UuidDistributionCondition.apply(1 to 100).applies(info) must be(true)
      }
    }
  }
}

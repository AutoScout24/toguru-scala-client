package toguru.impl

import java.util.UUID

import org.scalatest.{ShouldMatchers, WordSpec}
import toguru.api.ClientInfo

class UuidDistributionConditionSpec extends WordSpec with ShouldMatchers {

  "Invalid ranges" should {
    "Lower boundary is too low" in {

      intercept[IllegalArgumentException] {
        UuidDistributionCondition.apply(0 to 10)
      }.getMessage shouldBe "Range should describe a range between 1 and 100 inclusive"
    }

    "Upper boundary is too high" in {
      intercept[IllegalArgumentException] {
        UuidDistributionCondition.apply(90 to 101)
      }.getMessage shouldBe "Range should describe a range between 1 and 100 inclusive"
    }
  }

  "Valid UUIDs" should {
    "be projected within max range" in {
      1 to 1000 foreach {
        _ =>
          val uuid = UUID.randomUUID()
          val info = ClientInfo(uuid = Some(uuid))
          UuidDistributionCondition.apply(1 to 100).applies(info) shouldBe true
      }
    }
  }
}

package toguru.impl

import java.util.{Locale, UUID}

import org.scalatest.{ShouldMatchers, WordSpec}
import toguru.api.ClientInfo

class ConditionsSpec extends WordSpec with ShouldMatchers {

  "Browser conditions" should {

    "return true if browser matches" in {
      val clientInfo = ClientInfo(Some("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"))

      UserAgentCondition(Set("Chrome")).applies(clientInfo) shouldBe true
    }

    "return false if browser does not match" in {
      val clientInfo = ClientInfo(Some("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"))

      UserAgentCondition(Set("MSIE")).applies(clientInfo) shouldBe false
    }
  }

  "Locale/Culture conditions" should {
    "apply correctly to de-DE locale" in {
      val clientInfo = ClientInfo(culture = Some(Locale.GERMANY))

      CultureCondition(Set(Locale.GERMANY)).applies(clientInfo) shouldBe true
    }

    "apply to lang german germany locale de-DE from client" in {
      val clientInfo = ClientInfo(culture = Some(Locale.GERMANY))

      CultureCondition(Set(new Locale("", "DE"))).applies(clientInfo) shouldBe true
    }
  }

  "Uuid distribution conditions" should {
    val uuidWithDefaultProjectionToFive = UUID.fromString("56ed135b-a474-41d4-bde1-bc5e1e8bf910")

    "apply to true if uuid is projected into the given range" in {
      val clientInfo = ClientInfo(uuid = Some(uuidWithDefaultProjectionToFive))
      UuidDistributionCondition(3 to 5).applies(clientInfo) shouldBe true
    }

    "apply to false if uuid is projected outside the given range" in {
      val clientInfo = ClientInfo(uuid = Some(uuidWithDefaultProjectionToFive))
      UuidDistributionCondition(10 to 11).applies(clientInfo) shouldBe false
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
      projected shouldBe >(0)
      projected shouldBe <=(100)
    }
  }
}

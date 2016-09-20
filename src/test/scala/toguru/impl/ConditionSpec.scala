package toguru.impl

import java.util.{UUID, Locale}

import toguru.ClientInfo
import org.scalatest.FeatureSpec

class ConditionSpec extends FeatureSpec {

  feature("Browser conditions") {

    scenario("Browser condition returns true if browser matches") {
      val clientInfo = ClientInfo(Some("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"))
      assert(UserAgentCondition(Set("Chrome")).applies(clientInfo))
    }

    scenario("Browser condition returns false if browser does not match") {
      val clientInfo = ClientInfo(Some("Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"))
      assert(UserAgentCondition(Set("MSIE")).applies(clientInfo) === false)
    }
  }

  feature("Locale/Culture conditions") {
    scenario("de-DE locale correctly applies") {
      val clientInfo = ClientInfo(culture = Some(Locale.GERMANY))
      assert(CultureCondition(Set(Locale.GERMANY)).applies(clientInfo))
    }

    scenario("Culture condition for lang german applies to germany locale de-DE from client") {
      val clientInfo = ClientInfo(culture = Some(Locale.GERMANY))
      assert(CultureCondition(Set(new Locale("", "DE"))).applies(clientInfo))
    }
  }

  feature("Uuid distribution conditions") {
    val uuidWithDefaultProjectionToFive = UUID.fromString("56ed135b-a474-41d4-bde1-bc5e1e8bf910")

    scenario("Uuid distribution applies to true if uuid is projected into the given range") {
      val clientInfo = ClientInfo(uuid = Some(uuidWithDefaultProjectionToFive))
      assert(UuidDistributionCondition(3 to 5).applies(clientInfo))
    }

    scenario("Uuid distribution applies to false if uuid is projected outside the given range") {
      val clientInfo = ClientInfo(uuid = Some(uuidWithDefaultProjectionToFive))
      assert(!UuidDistributionCondition(10 to 11).applies(clientInfo))
    }

    scenario("Uuid distribution condition throws an exception if the range is not between 1 and 100") {
      intercept[IllegalArgumentException] {
        UuidDistributionCondition(100 to 101)
      }
    }
  }

  feature("default uuid to int projection") {
    val uuid = UUID.randomUUID()

    scenario(s"Random uuid ($uuid) gets projected between 1 and 100") {
      val projected = UuidDistributionCondition.defaultUuidToIntProjection(uuid)
      assert(projected > 0 && projected <= 100)
    }
  }
}

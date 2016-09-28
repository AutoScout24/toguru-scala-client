package toguru.impl

import org.scalatest.OptionValues._
import org.scalatest._
import toguru.api._

class TogglesStringSpec extends WordSpec with ShouldMatchers {

  "Parsing of forced features string" should {

    val forcedTogglesString = "feature1=true|feature2=false|feature3=true"
    val forcedToggles = TogglesString.parseForcedTogglesString(forcedTogglesString)

    "Existing features are correctly forced" in {
      forcedToggles("feature1").value shouldBe true
      forcedToggles("feature2").value shouldBe false
      forcedToggles("feature3").value shouldBe true
    }

    "Non existant forced features return None" in {
      forcedToggles("i do not exist") shouldBe None
    }

    "Feature forcing is case insensitive" in {
      forcedToggles("FEATURE1").value shouldBe true
    }

    "Illegal feature spec returns None for feature specified wrongly" in {
      val forcedFeaturesString = "feature1=thisiswrong|feature2=false|feature3=true"
      val forcedFeatures = TogglesString.parseForcedTogglesString(forcedFeaturesString)

      forcedFeatures("feature1") shouldBe None
      forcedFeatures("feature3").value shouldBe true
    }
  }

  "Can build feature strings from features and client info" should {

    "returns empty string when there are no features" in {
      implicit val clientInfo: ClientInfo = ClientInfo()
      TogglesString.buildTogglesString(Map.empty) shouldBe ""
    }

    "returns the correct feature when it is enforced to be true" in {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val toggleStates = Map("feature1" -> true)

      TogglesString.buildTogglesString(toggleStates) shouldBe "feature1=true"
    }

    "returns the disabled feature when it is enforced to be false" in {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val toggleStates = Map("feature1" -> false)

      TogglesString.buildTogglesString(toggleStates) shouldBe "feature1=false"
    }


    "returns multiple features separated by a pipe" in {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val toggleStates = Map("feature1" -> false, "feature2" -> true)

      TogglesString.buildTogglesString(toggleStates) shouldBe "feature1=false|feature2=true"
    }
  }

}

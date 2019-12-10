package toguru.impl

import org.scalatest.OptionValues._
import org.scalatest._
import toguru.api._

class TogglesStringSpec extends WordSpec with MustMatchers {

  "Parsing of forced features string" should {

    val forcedTogglesString = "feature1=true|feature2=false|feature3=true"
    val forcedToggles       = TogglesString.parse(forcedTogglesString)

    "Existing features are correctly forced" in {
      forcedToggles("feature1").value mustBe true
      forcedToggles("feature2").value mustBe false
      forcedToggles("feature3").value mustBe true
    }

    "Non existant forced features return None" in {
      forcedToggles("i do not exist") mustBe None
    }

    "Feature forcing is case insensitive" in {
      forcedToggles("FEATURE1").value mustBe true
    }

    "Illegal feature spec returns None for feature specified wrongly" in {
      val forcedFeaturesString = "feature1=thisiswrong|feature2=false|feature3=true"
      val forcedFeatures       = TogglesString.parse(forcedFeaturesString)

      forcedFeatures("feature1") mustBe None
      forcedFeatures("feature3").value mustBe true
    }
  }

  "Can build feature strings from features and client info" should {

    "returns empty string when there are no features" in {
      implicit val clientInfo: ClientInfo = ClientInfo()
      TogglesString.build(Map.empty) mustBe ""
    }

    "returns the correct feature when it is enforced to be true" in {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val toggleStates                    = Map("feature1" -> true)

      TogglesString.build(toggleStates) mustBe "feature1=true"
    }

    "returns the disabled feature when it is enforced to be false" in {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val toggleStates                    = Map("feature1" -> false)

      TogglesString.build(toggleStates) mustBe "feature1=false"
    }

    "returns multiple features separated by a pipe" in {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val toggleStates                    = Map("feature1" -> false, "feature2" -> true)

      TogglesString.build(toggleStates) mustBe "feature1=false|feature2=true"
    }
  }

}

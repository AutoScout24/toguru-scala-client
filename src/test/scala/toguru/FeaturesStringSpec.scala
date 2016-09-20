package toguru

import toguru.api._
import toguru.helpers.ClientInfoHelper
import toguru.impl._
import org.scalatest._
import org.scalatest.OptionValues._
import ClientInfoHelper._

class FeaturesStringSpec extends FeatureSpec with ShouldMatchers {

  feature("Parsing of forced features string") {

    val forcedFeaturesString = "feature1=true|feature2=false|feature3=true"
    val forcedFeatures = FeaturesString.parseForcedFeaturesString(forcedFeaturesString)

    scenario("Existing features are correctly forced") {
      assert(forcedFeatures("feature1").value === true)
      assert(forcedFeatures("feature2").value === false)
      assert(forcedFeatures("feature3").value === true)
    }

    scenario("Non existant forced features return None") {
      assert(forcedFeatures("i do not exist") === None)
    }

    scenario("Feature forcing is case insensitive") {
      assert(forcedFeatures("FEATURE1").value === true)
    }

    scenario("Illegal feature spec returns None for feature specified wrongly") {
      val forcedFeaturesString = "feature1=thisiswrong|feature2=false|feature3=true"
      val forcedFeatures = FeaturesString.parseForcedFeaturesString(forcedFeaturesString)

      assert(forcedFeatures("feature1") === None)
      assert(forcedFeatures("feature3").value === true)
    }
  }

  feature("Can build feature strings from features and client info") {

    scenario("returns empty string when there are no features") {
      implicit val clientInfo: ClientInfo = ClientInfo()
      FeaturesString.buildFeaturesString(Seq[Feature]()) should be("")
    }

    scenario("returns the correct feature when it is enforced to be true") {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val features: Seq[Feature] = Seq(
        new FeatureImpl(FeatureDescription("feature1", "it's a feature", None, Set(AlwaysOnCondition)))
      )

      FeaturesString.buildFeaturesString(features) should be("feature1=true")
    }

    scenario("returns the disabled feature when it is enforced to be false") {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val features: Seq[Feature] = Seq(
        new FeatureImpl(FeatureDescription("feature1", "it's a feature", None, Set(AlwaysOffCondition)))
      )
      FeaturesString.buildFeaturesString(features) should be("feature1=false")
    }

    scenario("returns the correct feature when enforced to be false but the client overrides to be true") {
      implicit val clientInfo: ClientInfo = ClientInfo(None, None, None, forceFeatureTo("feature1", enabled = true))
      val features: Seq[Feature] = Seq(
        new FeatureImpl(FeatureDescription("feature1", "it's a feature", None, Set(AlwaysOffCondition)))
      )
      FeaturesString.buildFeaturesString(features) should be("feature1=true")
    }

    scenario("returns the disabled feature when enforced to be true but the client overrides to be false") {
      implicit val clientInfo: ClientInfo = ClientInfo(None, None, None, forceFeatureTo("feature1", enabled = false))
      val features: Seq[Feature] = Seq(
        new FeatureImpl(FeatureDescription("feature1", "it's a feature", None, Set(AlwaysOnCondition)))
      )
      FeaturesString.buildFeaturesString(features) should be("feature1=false")
    }

    scenario("returns multiple features separated by a pipe") {
      implicit val clientInfo: ClientInfo = ClientInfo()
      val features: Seq[Feature] = Seq(
        new FeatureImpl(FeatureDescription("feature1", "it's a feature", None, Set(AlwaysOffCondition))),
        new FeatureImpl(FeatureDescription("feature2", "it's another feature", None, Set(AlwaysOnCondition)))
      )

      FeaturesString.buildFeaturesString(features) should be("feature1=false|feature2=true")
    }
  }

}

package toguru.impl

import org.scalatest.{FeatureSpec, MustMatchers, OptionValues}
import toguru.ClientInfoImpl

class AlwaysOffFeatureRegistrySpec extends FeatureSpec with OptionValues with MustMatchers {

  feature("Features are always toggled off for AlwaysOffFeatureRegistry") {
    scenario("'xyz' feature is toggled off") {
      AlwaysOffFeatureRegistry.feature("xyz").value.isActive(ClientInfoImpl()) must be(false)
    }
  }

  feature("feature names") {
    scenario("return empty set") {
      AlwaysOffFeatureRegistry.allFeatures must be(Set.empty)
    }
  }
}

package toguru.impl

import toguru.api.Feature.FeatureName
import toguru.api.{AlwaysOffFeature, Feature, FeatureRegistry}

object AlwaysOffFeatureRegistry extends FeatureRegistry {
  override def feature(name: String): Option[Feature] = Some(AlwaysOffFeature(name))
  override def allFeatures: Set[(FeatureName, Feature)] = Set()
}

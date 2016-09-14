package toguru.impl

import toguru.impl.FeatureDescriptionHelper.Tag

case class FeatureDescription(name: String, description: String, tags: Option[Set[Tag]], activation: Set[Condition], services: Option[Set[String]] = None) {
  if(name.trim.length == 0) throw new IllegalArgumentException("Name of feature must not be empty")
  if(activation.isEmpty) throw new IllegalArgumentException("""conditions for activation may not be empty, consider using AlwaysOn/AlwaysOff (in json: "default" : true/false) if you want to statically switch features on or off""")
}

object FeatureDescriptionHelper {
  type Tag = String
}
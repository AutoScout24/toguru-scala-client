package toguru.impl

import toguru.api.Toggle.ToggleId

import scala.util.Try

object TogglesString {

  private def lowerCaseKeys[T](m: Map[String,T]) = m.map { case (k, v) => (k.toLowerCase, v) }

  /**
   * Parses the string for a forced activation of features.
   *
   * @param togglesString usually has a format like: feature1=true|feature2=false|feature3=true, where the feature name
    *                       should be case insensitive
    * @return a function that can check if a given feature should be forced to be in the given state (true| false). The
    *         feature name is case insensitive.
   */
  def parse(togglesString: String): ToggleId => Option[Boolean] = {
    featureName =>
      val map = togglesString.split('|').toList.flatMap {
        singleFeature =>
          singleFeature.split('=').toList match {
            case key :: value :: Nil if Try(value.toBoolean).isSuccess => Some(key.toLowerCase -> value.toBoolean)
            case other => None // wrong feature format. e.g. name=uh
          }
      }.toMap

      lowerCaseKeys(map).get(featureName.toLowerCase)
  }

  /**
    * Builds a feature string from a set of features and the current clientInfo.
    *
    * @param toggles The toggles to build the string from.
    * @return A feature string in the format of feature1=true|feature2=false|feature3=true. Where all feature objects in
    *         `features` are covered in the output.
    */
  def build(toggles: Map[ToggleId,Boolean]): String = toggles.map { case (id, on) => s"$id=$on" }.mkString("|")

}

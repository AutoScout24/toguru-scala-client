package toguru.helpers

import toguru.api.Toggle.ToggleId

object ClientInfoHelper {

  /**
    * Helper function to force a feature to a particular state
    * @param toggleId The identifier name for the feature
    * @param enabled Should this feature be forced to be enabled or disabled?
    * @return A function that forces `featureName` to be a particular state
    */
  def forceToggleTo(toggleId: ToggleId, enabled: Boolean): (ToggleId => Option[Boolean]) = { id =>
    if (id == toggleId) Some(enabled) else None
  }
}

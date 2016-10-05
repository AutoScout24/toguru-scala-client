package toguru.api

import toguru.api.Toggle.ToggleId


object Toggle {
  type ToggleId = String
}

/**
  * A handle for a toggle.
  *
  * This class defines a fallback activation condition to use if the remote activation provider cannot be reached.
  * The id of the toggle is used to identify the activation conditions to apply from the remote activation provider.
  *
  * For usage examples, see [[toguru.play.PlaySupport]]
  *
  * @param id the toggle id
  * @param default the default toggle condition.
  */
case class Toggle(id: ToggleId, default: Condition = Condition.Off) {

  /**
    * Returns whether the toggle is on
    * @param toggling the required toggling information to make the toggle decision.
    * @return
    */
  def isOn(implicit toggling: Toggling): Boolean = toggling.apply(this)

  /**
    * Returns whether the toggle is off
    * @param toggling the required toggling information to make the toggle decision.
    * @return
    */
  def isOff(implicit toggling: Toggling): Boolean = !toggling.apply(this)

}

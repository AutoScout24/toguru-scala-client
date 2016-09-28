package toguru.api

import toguru.api.Toggle.ToggleId


object Toggle {
  type ToggleId = String
}

case class Toggle(id: ToggleId, default: Condition = Condition.Off) {

  def isOn(implicit toggling: Toggling): Boolean = toggling(this)

  def isOff(implicit toggling: Toggling): Boolean = !toggling(this)

}

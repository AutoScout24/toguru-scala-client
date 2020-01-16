package toguru.implicits

import toguru.api.Toggling
import toguru.impl.{ToggleState, TogglesString}

object toggle {

  implicit class TogglesToString(toggles: Iterable[ToggleState]) {

    def buildString(implicit toggling: Toggling): String = TogglesString.build {
      import toggling.client
      toggles.map(t => t.id -> client.forcedToggle(t.id).getOrElse(t.condition.applies(client)))
    }

  }

}

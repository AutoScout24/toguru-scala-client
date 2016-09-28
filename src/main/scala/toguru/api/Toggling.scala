package toguru.api

import toguru.impl.TogglesString

trait Toggling {

  def client: ClientInfo

  def activations: Activations

  def apply(toggle: Toggle): Boolean =
    client.forcedToggle(toggle.id).getOrElse(activations(toggle).applies(client))

  def toggleStringForService(service: String): String = {
    val toggleStates = activations.togglesFor(service)
      .map { case (id, c) => id -> client.forcedToggle(id).getOrElse(c.applies(client))}

    TogglesString.buildTogglesString(toggleStates)
  }
}

case class TogglingInfo(client: ClientInfo, activations: Activations) extends Toggling

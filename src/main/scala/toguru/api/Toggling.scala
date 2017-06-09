package toguru.api

import toguru.impl.{ToggleState, TogglesString}

/**
  * Contains the information needed by [[toguru.api.Toggle]]s to make toggling decisions.
  */
trait Toggling {

  def client: ClientInfo

  def activations: Activations

  /**
    * Returns the activation state of the given toggle.
    *
    * @param toggle the toggle in question
    * @return
    */
  def apply(toggle: Toggle): Boolean =
    client.forcedToggle(toggle.id).getOrElse(activations(toggle).applies(client))

  /**
    * Returns all activations.
    *
    * @return
    */
  def apply(): Traversable[ToggleState] = activations()

  /**
    * Returns a toggling string for downstream services.
    *
    * @param service the name of the downstream service - must match the "services" tag defined on the toggle on
    *                the toguru server.
    * @return
    */
  @deprecated("It will be removed, use apply(s: String) instead", "1.0.2")
  def toggleStringForService(service: String): String = {
    // $COVERAGE-OFF$Disabled because it's deprecated
    val toggleStates = activations.togglesFor(service)
      .map { case (id, c) => id -> client.forcedToggle(id).getOrElse(c.applies(client))}

    TogglesString.build(toggleStates)
    // $COVERAGE-ON$
  }
}

/**
  * Simple case class-based implementation of the Toggling trait. See also [[toguru.api.ToguruClient#apply]]
  *
  *
  * @param client the client information to use
  * @param activations the activation conditions to use
  */
case class TogglingInfo(client: ClientInfo, activations: Activations) extends Toggling

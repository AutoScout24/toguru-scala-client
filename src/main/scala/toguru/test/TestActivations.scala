package toguru.test

import toguru.api.{Activations, Condition, Toggle}

/**
  * A class for providing toggle activations to toggled code in tests
  */
object TestActivations {

  def apply(activations: (Toggle, Condition)*)(services: (Toggle, String)*) = new Activations.Provider() {
    override def apply() = new Impl(activations: _*)(services: _*)

    override def healthy() = true
  }

  class Impl(activations: (Toggle, Condition)*)(services: (Toggle, String)*) extends Activations {

    override def apply(toggle: Toggle) = activations.collectFirst { case (`toggle`, c) => c }.getOrElse(toggle.default)

    override def togglesFor(service: String) = services.collect { case (t, `service`) => t.id -> apply(t) }.toMap
  }
}

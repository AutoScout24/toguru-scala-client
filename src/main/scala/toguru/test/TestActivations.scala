package toguru.test

import toguru.api.Toggle._
import toguru.api.{Activations, Condition, Toggle}

/**
  * A class for providing toggle activations to toggled code in tests
  */
object TestActivations {

  def apply(activations: (Toggle, Condition)*)(services: (Toggle, String)*) = new Activations.Provider() {
    val tags = services.map {
      case (toggle, service) => (toggle, Map("services" -> service))
    }

    override def apply() = new Impl(activations: _*)(tags: _*)

    override def healthy() = true
  }

  class Impl(activations: (Toggle, Condition)*)(tags: (Toggle, Map[String, String])*) extends Activations {

    override def apply(toggle: Toggle) = activations.collectFirst { case (`toggle`, c) => c }.getOrElse(toggle.default)

    def togglesForFilter(filter: (Map[String, String], Condition) => Boolean): Map[ToggleId, Condition] =
      activations.map {
        case (toggle, condition) => (toggle, condition, tags.toMap.getOrElse(toggle, Map.empty))
      }.collect {
        case (toggle, condition, tags) if filter(tags, condition) => toggle.id -> condition
      }.toMap

    override def stateSequenceNo: Option[Long] = None
  }
}

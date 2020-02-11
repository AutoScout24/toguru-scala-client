package toguru.test

import toguru.api.Toggle.ToggleId
import toguru.api.{Activations, Condition, Toggle}
import toguru.impl.ToggleState

/**
  * A class for providing toggle activations to toggled code in tests
  */
object TestActivations {

  def apply(activations: (Toggle, Condition)*)(services: (Toggle, String)*): Activations.Provider =
    new Activations.Provider() {
      override def apply() = new Impl(activations: _*)(services: _*)

      override def healthy() = true
    }

  private[toguru] class Impl(activations: (Toggle, Condition)*)(services: (Toggle, String)*) extends Activations {

    override def apply(toggle: Toggle): Condition =
      activations.collectFirst { case (`toggle`, c) => c }.getOrElse(toggle.default)

    override def apply(): Seq[ToggleState] = (activations.map(_._1) ++ services.map(_._1)).distinct.map { t =>
      new ToggleState(
        t.id,
        services.find(_._1 == t).map("service" -> _._2).toMap,
        activations.collectFirst { case (`t`, c) => c }.getOrElse(Condition.Off)
      )
    }

    override def togglesFor(service: String): Map[ToggleId, Condition] =
      services.collect { case (t, `service`) => t.id -> apply(t) }.toMap

    override def stateSequenceNo: Option[Long] = None
  }
}

package toguru.impl

import toguru.api.Toggle.ToggleId
import toguru.api.{Activations, Condition, Toggle}

case class ToggleStates(sequenceNo: Option[Long], toggles: Seq[ToggleState])

case class ToggleState(id: String, tags: Map[String, String], condition: Condition)

object ToggleState {

  def apply(id: String, tags: Map[String, String], activations: Seq[ToggleActivation]): ToggleState = {

    val condition: Condition = {
      val rollout = for {
        activation <- activations.headOption
        rollout    <- activation.rollout
      } yield Condition.UuidRange(1 to rollout.percentage)

      val attributes = for {
        activation <- activations.headOption.toSeq
        (k, v)     <- activation.attributes
      } yield Attribute(k, v)

      (attributes :+ rollout.getOrElse(Condition.Off)).toList match {
        case Seq(c) => c
        case cs     => All(cs.toSet)
      }
    }

    new ToggleState(id, tags, condition)
  }

}

case class ToggleActivation(rollout: Option[Rollout] = None, attributes: Map[String, Seq[String]] = Map.empty)

case class Rollout(percentage: Int)

class ToggleStateActivations(toggleStates: ToggleStates) extends Activations {

  val conditions = toggleStates.toggles.map(ts => ts.id -> ts.condition).toMap

  override def apply(toggle: Toggle) = conditions.getOrElse(toggle.id, toggle.default)

  override def apply(): Iterable[ToggleState] = toggleStates.toggles

  override def togglesFor(service: String): Map[ToggleId, Condition] =
    toggleStates.toggles
      .filter { toggle =>
        toggle.tags.get("services").toVector.flatMap(_.split(",")).exists(_.trim == service) ||
        toggle.tags.get("service").exists(_.trim == service)
      }
      .map(toggle => toggle.id -> conditions(toggle.id))
      .toMap

  override def stateSequenceNo: Option[Long] = toggleStates.sequenceNo
}

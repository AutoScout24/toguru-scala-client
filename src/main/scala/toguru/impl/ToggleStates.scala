package toguru.impl

import toguru.api.{Activations, Condition, Toggle}

case class ToggleStates(sequenceNo: Option[Long], toggles: Seq[ToggleState])

case class ToggleState(id: String, tags: Map[String, String], activations: Seq[ToggleActivation])

case class ToggleActivation(rollout: Option[Rollout] = None, attributes: Map[String, Seq[String]] = Map.empty)

case class Rollout(percentage: Int)

class ToggleStateActivations(toggleStates: ToggleStates) extends Activations {

  def activationConditions(toggleState: ToggleState): (String, Condition) = {
    val rollout = for {
      activation <- toggleState.activations.headOption
      rollout <- activation.rollout
    } yield Condition.UuidRange(1 to rollout.percentage)

    val attributes = for {
      activation <- toggleState.activations.headOption.to[Seq]
      (k, v) <- activation.attributes
    } yield Attribute(k, v)

    val condition = (attributes ++ rollout).to[List] match {
      case Nil    => Condition.Off
      case Seq(c) => c
      case cs     => All(cs.to[Set])
    }

    toggleState.id -> condition
  }

  val conditions = toggleStates.toggles.map(activationConditions).toMap

  override def apply(toggle: Toggle) = conditions.getOrElse(toggle.id, toggle.default)

  override def togglesFor(service: String) = {
    val toggles =
      for {
        toggle <- toggleStates.toggles if toggle.tags.get("services").contains(service)
        id = toggle.id
      } yield (id, conditions(id))
    toggles.toMap
  }

  override def stateSequenceNo: Option[Long] = toggleStates.sequenceNo
}

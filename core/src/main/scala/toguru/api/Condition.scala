package toguru.api

import toguru.impl.{All, AlwaysOffCondition, AlwaysOnCondition, UuidDistributionCondition, Attribute => Att}

trait Condition {
  def applies(clientInfo: ClientInfo): Boolean
}

object Condition {

  val On: Condition  = AlwaysOnCondition
  val Off: Condition = AlwaysOffCondition

  def UuidRange(range: Range): Condition =
    UuidDistributionCondition(List(range), UuidDistributionCondition.defaultUuidToIntProjection)

  def Attribute(name: String, values: String*): Condition = Att(name, values)

  def apply(conditions: Condition*): Condition = conditions match {
    case Nil    => Condition.On
    case Seq(c) => c
    case cs     => All(cs.toSet)
  }
}

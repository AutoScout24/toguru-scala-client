package toguru.api

import toguru.impl.{AlwaysOffCondition, AlwaysOnCondition, UuidDistributionCondition}

trait Condition {
  def applies(clientInfo: ClientInfo): Boolean
}

object Condition {

  val On:  Condition = AlwaysOnCondition
  val Off: Condition = AlwaysOffCondition

  def UuidRange(range: Range): Condition =
    UuidDistributionCondition(List(range), UuidDistributionCondition.defaultUuidToIntProjection)

}
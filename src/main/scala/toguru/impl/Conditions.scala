package toguru.impl

import java.math.BigInteger
import java.util.UUID

import toguru.api.{ClientInfo, Condition}

case object AlwaysOnCondition extends Condition {
  override def applies(clientInfo: ClientInfo): Boolean = true
}

case object AlwaysOffCondition extends Condition {
  override def applies(clientInfo: ClientInfo): Boolean = false
}

case class All(conditions: Set[Condition]) extends Condition {
  override def applies(clientInfo: ClientInfo) = conditions.forall(_.applies(clientInfo))
}

case class Attribute(name: String, values: Seq[String]) extends Condition {
  override def applies(clientInfo: ClientInfo) = clientInfo.attributes.get(name).exists(values.contains)
}

/**
  * A condition that takes the uuid from client info, applies f() to it and checks if it is inside the given range.
  *
  * @param f      a function that projects an UUID to an Int in the range between 1 and 100
  * @param ranges ranges from 1 to 100 (inclusive) that f(uuid) has to land in so that this condition will be true for the client uuid
  */
case class UuidDistributionCondition(ranges: Seq[Range], f: UUID => Int) extends Condition {

  ranges.foreach(r => if (r.head < 1 || r.last > 100) throw new IllegalArgumentException("Range should describe a range between 1 and 100 inclusive"))

  override def applies(clientInfo: ClientInfo): Boolean = {
    clientInfo.uuid match {
      case Some(uuid) =>
        val projected = f(uuid)
        ranges.exists(range => {
          range.contains(projected)
        })
      case None => false
    }
  }
}

object UuidDistributionCondition {

  def apply(range: Range, f: UUID => Int = defaultUuidToIntProjection): UuidDistributionCondition = apply(Seq(range), f)

  val defaultUuidToIntProjection: UUID => Int = {
    (uuid) =>
      val hibits = uuid.getMostSignificantBits
      val lobits = uuid.getLeastSignificantBits
      val barrayHi = BigInteger.valueOf(hibits).toByteArray
      val barrayLo = BigInteger.valueOf(lobits).toByteArray
      val comb = barrayLo ++ barrayHi
      Math.abs(new BigInteger(comb).mod(BigInteger.valueOf(100l)).intValue()) + 1
  }
}

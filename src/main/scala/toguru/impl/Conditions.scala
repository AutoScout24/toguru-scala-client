package toguru.impl

import java.math.BigInteger
import java.util.{Locale, UUID}

import toguru.impl.LocaleSupport._
import toguru.api.{ClientInfo, Condition}
import toguru.api.ClientInfo.UserAgent

case object AlwaysOnCondition extends Condition {
  override def applies(clientInfo: ClientInfo): Boolean = true
}

case object AlwaysOffCondition extends Condition {
  override def applies(clientInfo: ClientInfo): Boolean = false
}

/**
 * @param userAgentFragments one (only one, not all!) of this fragments should be contained in the user agent header to make this condition apply.
 */
case class UserAgentCondition(userAgentFragments: Set[UserAgent]) extends Condition {
  override def applies(clientInfo: ClientInfo): Boolean = clientInfo.userAgent.exists(ua => userAgentFragments.exists(uaFrag => ua.contains(uaFrag)))
}

case class CultureCondition(cultures: Set[Locale]) extends Condition {
  override def applies(clientInfo: ClientInfo): Boolean = {
    cultures.exists {
      activatingLocale => activatingLocale.lang match {
        case None =>
          clientInfo.culture.exists(clientLocale => activatingLocale.country == clientLocale.country)
        case _ => clientInfo.culture.contains(activatingLocale)
      }
    }
  }
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
    // if no uuid is set use a random, but be aware that the feature is NOT stable for the client
    val uuid = clientInfo.uuid.getOrElse(UUID.randomUUID())
    val projected = f(uuid)
    ranges.exists(range => {
      range.contains(projected)
    })
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

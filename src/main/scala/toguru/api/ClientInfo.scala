package toguru.api

import java.util.{Locale, UUID}

import toguru.api.ClientInfo.UserAgent
import toguru.api.Toggle.ToggleId

case class ClientInfo(
             userAgent: Option[UserAgent] = None,
             culture: Option[Locale] = None,
             uuid: Option[UUID] = None,
             forcedToggle: ToggleId => Option[Boolean] = (_) => None)

object ClientInfo {
  type UserAgent = String

  type Provider[T] = T => ClientInfo

}
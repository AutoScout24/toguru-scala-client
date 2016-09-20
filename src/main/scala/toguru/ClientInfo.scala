package toguru

import java.util.{UUID, Locale}
import toguru.ClientInfo.UserAgent
import toguru.api.Feature
import Feature.FeatureName

case class ClientInfo(userAgent: Option[UserAgent] = None, culture: Option[Locale] = None, uuid: Option[UUID] = None,
                          forcedFeatureToggle: FeatureName => Option[Boolean] = (_) => None)

object ClientInfo {
  type UserAgent = String
}
package toguru.impl

import java.time.LocalDateTime

import play.api.libs.json.Json
import toguru.api.{Feature, FeatureImpl, FeatureRegistry}
import toguru.impl.PollingRegistryUpdater.{EventPublisher, TogglePoller}

import scala.util.{Failure, Success, Try}
import scalaj.http.Http

case class ToggleState(id: String,
                       tags: Map[String, String] = Map.empty,
                       rolloutPercentage: Option[Int] = None)

object PollingRegistryUpdater {
  type EventPublisher = (String, (String, Any)*) => Unit

  type TogglePoller = () => (Int, String)

  def apply(endpointUrl: String, publisher: EventPublisher): PollingRegistryUpdater = {
    val poller: TogglePoller = () => {
      val response = Http(endpointUrl).timeout(500, 750).asString
      (response.code, response.body)
    }

    new PollingRegistryUpdater(poller, publisher)
  }
}

class PollingRegistryUpdater(poller: TogglePoller, publisher: EventPublisher) {

  implicit val toggleReads = Json.reads[ToggleState]

  def apply(): Option[(FeatureRegistry, LocalDateTime)] = {
    val dateTime = LocalDateTime.now()
    Try(poller()) match {
      case Success((code, body)) =>
        val tryToggleStates = Try(Json.parse(body).as[Seq[ToggleState]])
        (code, tryToggleStates) match {
          case (200, Success(toggleStates)) =>
            Some((new ToggleStateFeatureRegistry(toggleStates), dateTime))

          case _ =>
            publisher("registry-poll-failed", "responseCode" -> code, "responseBody" -> body)
            None
        }
      case Failure(e) =>
        publisher("registry-poll-failed", "message" -> e.getMessage)
        None
    }
  }
}

class ToggleStateFeatureRegistry(toggleStates: Seq[ToggleState]) extends FeatureRegistry {

  val features: Map[String, Feature] = toggleStates.map(t => t.id -> new FeatureImpl(toFeatureDescription(t))).toMap

  def toFeatureDescription(toggleState: ToggleState) =
    FeatureDescription(
      toggleState.id,
      "",
      Some(toggleState.tags.values.to[Set]),
      activationConditions(toggleState),
      toggleState.tags.get("services").map(s => Set(s)))

  def activationConditions(toggleState: ToggleState): Set[Condition] = toggleState.rolloutPercentage match {
    case Some(p) =>
      Set(UuidDistributionCondition(1 to p, UuidDistributionCondition.defaultUuidToIntProjection))
    case None    => Set(AlwaysOffCondition)
  }

  override def feature(name: String) = features.get(name)

  override def allFeatures = features.to[Set]
}

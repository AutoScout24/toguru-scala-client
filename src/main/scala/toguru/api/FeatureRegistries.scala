package toguru.api

import java.time.LocalDateTime

import scala.concurrent.duration._
import akka.actor.Scheduler
import toguru.impl.{AlwaysOffFeatureRegistry, PollingRegistryUpdater, ReloadingFeatureRegistry}

import scala.concurrent.ExecutionContext

object FeatureRegistries {

  type EventPublisher = (String, (String, Any)*) => Unit

  def fromEndpoint(
            endpointUrl: String,
            scheduler: Scheduler,
            publisher: EventPublisher,
            executor: ExecutionContext): FeatureRegistry = {

    val updater = PollingRegistryUpdater(endpointUrl, publisher)
    new ReloadingFeatureRegistry(
      initial = (AlwaysOffFeatureRegistry, LocalDateTime.now()),
      reCreator = () => updater(),
      scheduler = scheduler,
      reloadAfter = 5.seconds,
      activationDelay = 3.seconds,
      executor = executor
    )
  }
}

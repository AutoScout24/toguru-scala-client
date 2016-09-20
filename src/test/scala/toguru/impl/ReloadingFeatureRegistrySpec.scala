package toguru.impl

import java.time.LocalDateTime

import akka.actor.ActorSystem
import toguru.ClientInfo
import toguru.api.Feature.FeatureName
import toguru.api.{AlwaysOffFeature, AlwaysOnFeature, Feature, FeatureRegistry}
import org.scalatest.concurrent.{Eventually, IntegrationPatience}
import org.scalatest.{BeforeAndAfterAll, FeatureSpec, MustMatchers, OptionValues}

import scala.concurrent.duration._
import scala.language.postfixOps

class ReloadingFeatureRegistrySpec extends FeatureSpec with BeforeAndAfterAll with OptionValues with MustMatchers with Eventually with IntegrationPatience {

  val testActorSystem = ActorSystem()
  override def afterAll(): Unit = testActorSystem.terminate()

  feature("Reloading of feature registry works") {
    scenario("Recreator function of feature registry really get's called and is used") {

      val initialFeatureRegistry = new FeatureRegistry {
        override def allFeatures: Set[(FeatureName, Feature)] = ???
        override def feature(name: String): Option[Feature] = Some(AlwaysOffFeature(name))
      }

      val subsequentFeatureRegistry = new FeatureRegistry {
        override def allFeatures: Set[(FeatureName, Feature)] = ???
        override def feature(name: String): Option[Feature] = Some(AlwaysOnFeature(name))
      }

      val initial = (initialFeatureRegistry, LocalDateTime.MIN)

      val now = LocalDateTime.now()
      val subsequent: () => Option[(FeatureRegistry, LocalDateTime)] = () => {
        Some((subsequentFeatureRegistry, now))
      }

      val reloadingFeatureRegistry =
        new ReloadingFeatureRegistry(initial, subsequent, testActorSystem.scheduler, 1 seconds, 2 seconds, scala.concurrent.ExecutionContext.Implicits.global)

      val clientInfo = ClientInfo()
      reloadingFeatureRegistry.feature("featureName").value.isActive(clientInfo) must be(false)

      eventually {
        reloadingFeatureRegistry.feature("featureName").value.isActive(clientInfo) must be(true)
      }
    }
  }
}
package toguru.impl

import java.net.ServerSocket
import java.util.concurrent.Executors

import org.http4s.server.blaze.BlazeBuilder
import org.scalatest.{OptionValues, ShouldMatchers, WordSpec}
import toguru.api.{Condition, DefaultActivations, Toggle}
import toguru.impl.RemoteActivationsProvider.TogglePoller

import scala.concurrent.duration._

class RemoteActivationsProviderSpec extends WordSpec with OptionValues with ShouldMatchers {

  val toggleOne = Toggle("toggle-one")
  val toggleTwo = Toggle("toggle-two")
  val executor = Executors.newSingleThreadScheduledExecutor()

  def createProvider(poller: TogglePoller): RemoteActivationsProvider =
    new RemoteActivationsProvider(poller, executor).close()

  "Fetching features from toggle endpoint" should {
    "succeed if toggle endpoint is available" in {
      val response =
        """
          |[
          |  { "id": "toggle-one", "tags": {"services": "toguru"}},
          |  { "id": "toggle-two", "tags": {"team": "Shared Services"}, "rolloutPercentage": 20}
          |]
        """.stripMargin

      val poller: TogglePoller = () => (200, response)

      val provider = createProvider(poller)

      val toggles = provider.fetchToggleStates().value
      provider.update()
      val activations = provider.apply()

      val toggleStateOne = toggles.collectFirst { case t if t.id == toggleOne.id => t }.value
      val toggleStateTwo = toggles.collectFirst { case t if t.id == toggleTwo.id => t }.value

      toggleStateOne.id shouldBe "toggle-one"
      toggleStateOne.tags shouldBe Map("services" -> "toguru")
      toggleStateOne.rolloutPercentage shouldBe None

      toggleStateTwo.id shouldBe "toggle-two"
      toggleStateTwo.tags shouldBe Map("team" -> "Shared Services")
      toggleStateTwo.rolloutPercentage shouldBe Some(20)

      activations.apply(toggleOne) shouldBe Condition.Off
    }

    "fail if toggle endpoint returns 500" in {
      val response = ""

      val poller: TogglePoller = () => (500, response)
      val provider = createProvider(poller)

      provider.fetchToggleStates() shouldBe None

      provider.update()
      provider.apply() shouldBe DefaultActivations
    }

    "fail if toggle endpoint returns malformed json" in {
      val response = "ok"

      val poller: TogglePoller = () => (200, response)
      val provider = createProvider(poller)

      provider.fetchToggleStates() shouldBe None

      provider.update()
      provider.apply() shouldBe DefaultActivations
    }

    "fail if poller throws exception" in {
      val poller: TogglePoller = () => throw new RuntimeException("boom")
      val provider = createProvider(poller)

      provider.fetchToggleStates() shouldBe None
    }
  }

  "Creating activation provider" should {
    "succeed with a valid url" in {
      val provider = RemoteActivationsProvider("http://localhost:9000")
      provider.close()
    }
  }

  "Created activation provider" should {
    "poll remote url" in {
      import org.http4s._
      import org.http4s.dsl._
      val service = HttpService {
        case _ => Ok("""[{"id":"toggle-one","tags":{"team":"Toguru Team","services":"toguru"},"rolloutPercentage":20}]""")
      }
      val port = freePort
      val server = BlazeBuilder.bindHttp(port, "localhost").mountService(service, "/togglestate").run

      val rolloutCondition = Condition.UuidRange(1 to 20)

      val provider = RemoteActivationsProvider(s"http://localhost:$port", pollInterval = 100.milliseconds)

      waitFor(100, 100.millis) {
        provider.apply() != DefaultActivations
      }

      provider.close()
      server.shutdownNow()

      val activations = provider.apply()

      activations.apply(toggleOne) shouldBe rolloutCondition
      activations.togglesFor("toguru") shouldBe Map(toggleOne.id -> rolloutCondition)
    }
  }


  /**
    *
    * @param times how many times we want to try.
    * @param wait how long to wait before the next try
    * @param test returns true if test (finally) succeeded, false if we need to retry
    */
  def waitFor(times: Int, wait: FiniteDuration = 2.second)(test: => Boolean): Unit = {
    val success = (1 to times).exists { i =>
      if(test) {
        true
      } else {
        if(i < times)
          Thread.sleep(wait.toMillis)
        false
      }
    }

    success shouldBe true
  }

  def freePort: Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }
}

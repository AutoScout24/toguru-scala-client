package toguru.impl

import java.net.ServerSocket
import java.util.concurrent.{Executors, TimeUnit}

import com.hootsuite.circuitbreaker.CircuitBreakerBuilder
import org.http4s._
import org.http4s.dsl._
import org.http4s.headers._
import org.http4s.server.blaze.BlazeBuilder
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{MustMatchers, OptionValues, WordSpec}
import toguru.api.{Condition, DefaultActivations, Toggle}
import toguru.impl.RemoteActivationsProvider.{PollResponse, TogglePoller}

import scala.concurrent.duration.{FiniteDuration, _}

class RemoteActivationsProviderSpec extends WordSpec with OptionValues with MustMatchers with MockitoSugar {

  val toggleOne = Toggle("toggle-one")
  val toggleTwo = Toggle("toggle-two")
  val executor = Executors.newSingleThreadScheduledExecutor()

  def poller(response: String, contentType: String = RemoteActivationsProvider.MimeApiV3): TogglePoller =
    _ => PollResponse(200, contentType, response)

  val circuitBreakerBuilder = CircuitBreakerBuilder(
    name = "test-breaker",
    failLimit  = 1000,
    retryDelay = FiniteDuration(100, TimeUnit.MILLISECONDS)
  )

  def createProvider(poller: TogglePoller): RemoteActivationsProvider =
    new RemoteActivationsProvider(poller, executor, circuitBreakerBuilder = circuitBreakerBuilder).close()

  def createProvider(response: String, contentType: String = RemoteActivationsProvider.MimeApiV3): RemoteActivationsProvider =
    createProvider(poller(response, contentType))

  "Fetching features from toggle endpoint" should {

    def validateResponse(toggles: Seq[ToggleState]): Unit = {
      val toggleStateOne = toggles.collectFirst { case t if t.id == toggleOne.id => t }.value
      val toggleStateTwo = toggles.collectFirst { case t if t.id == toggleTwo.id => t }.value

      toggleStateOne.id mustBe "toggle-one"
      toggleStateOne.tags mustBe Map("services" -> "toguru")
      toggleStateOne.condition mustBe Condition.Off

      toggleStateTwo.id mustBe "toggle-two"
      toggleStateTwo.tags mustBe Map("team" -> "Shared Services")
      toggleStateTwo.condition mustBe Condition.UuidRange(1 to 20)
    }

    "send sequenceNo to server" in {
      val poller = mock[TogglePoller]
      when(poller.apply(Some(10))).thenReturn(PollResponse(200, "", ""))

      val provider = createProvider(poller)

      provider.fetchToggleStates(Some(10))

      verify(poller).apply(Some(10))
    }

    "succeed if a toggle response is received" in {
      val response =
        """
          |{
          |  "sequenceNo": 10,
          |  "toggles": [
          |    { "id": "toggle-one", "tags": {"services": "toguru"}, "activations": [] },
          |    { "id": "toggle-two", "tags": {"team": "Shared Services"}, "activations": [ {"rollout": {"percentage": 20 }, "attributes": {} } ] }
          |  ]
          |}
          |""".stripMargin

      val provider = createProvider(response)

      val toggles = provider.fetchToggleStates(None).value.toggles

      validateResponse(toggles)
    }

    "keeps latest sequence number in activation conditions" in {
      val response =
        """
          |{
          |  "sequenceNo": 10,
          |  "toggles": []
          |}
        """.stripMargin

      val provider = createProvider(response)

      provider.update()
      val activations = provider.apply()

      activations.stateSequenceNo mustBe Some(10)
    }

    "rejects stale toggle state updates" in {
      val response =
        """
          |{
          |  "sequenceNo": 5,
          |  "toggles": []
          |}
        """.stripMargin

      val provider = createProvider(response)

      val maybeToggleStates = provider.fetchToggleStates(Some(10))

      maybeToggleStates mustBe None
    }

    "fails when server returns no sequence number, but client already has one" in {
      val response = """[]"""

      val provider = createProvider(response)

      val maybeToggleStates = provider.fetchToggleStates(Some(10))

      maybeToggleStates mustBe None
    }

    "succeed if a V2 toggle response is received" in {
      val response =
        """
          |{
          |  "sequenceNo": 10,
          |  "toggles": [
          |    { "id": "toggle-one", "tags": {"services": "toguru"}},
          |    { "id": "toggle-two", "tags": {"team": "Shared Services"}, "rolloutPercentage": 20}
          |  ]
          |}
        """.stripMargin

      val provider = createProvider(response, "")

      val toggles = provider.fetchToggleStates(None).value.toggles

      validateResponse(toggles)
    }

    "succeed if a V1 toggle response is received" in {
      val response =
        """
          |[
          |  { "id": "toggle-one", "tags": {"services": "toguru"}},
          |  { "id": "toggle-two", "tags": {"team": "Shared Services"}, "rolloutPercentage": 20}
          |]
        """.stripMargin

      val provider = createProvider(response, "")

      val toggles = provider.fetchToggleStates(None).value.toggles

      validateResponse(toggles)
    }

    "fail if toggle endpoint returns 500" in {
      val poller: TogglePoller = _ => PollResponse(500, "", "")
      val provider = createProvider(poller)

      provider.fetchToggleStates(None) mustBe None

      provider.update()
      provider.apply() mustBe DefaultActivations
    }

    "fail if toggle endpoint returns malformed json" in {
      val provider = createProvider("ok")

      provider.fetchToggleStates(None) mustBe None

      provider.update()
      provider.apply() mustBe DefaultActivations
    }

    "fail if poller throws exception" in {
      val poller: TogglePoller = _ => throw new RuntimeException("boom")
      val provider = createProvider(poller)

      provider.fetchToggleStates(None) mustBe None
    }
  }

  "Creating activation provider" should {
    "succeed with a valid url" in {
      val provider = RemoteActivationsProvider("http://localhost:9000")
      provider.close()
    }
  }

  "Created activation provider" should {
    def createProviderAndServer(service: HttpService) = {

      val port = freePort
      (RemoteActivationsProvider(s"http://localhost:$port", pollInterval = 100.milliseconds, circuitBreakerBuilder = circuitBreakerBuilder),
        BlazeBuilder.bindHttp(port, "localhost").mountService(service, "/togglestate").run)
    }

    "poll remote url" in {
      val service = HttpService {
        case _ => Ok(
          """
            |{
            |  "sequenceNo": 10,
            |  "toggles": [{"id":"toggle-one","tags":{"team":"Toguru Team","services":"toguru"},"rolloutPercentage":20}]
            |}""".stripMargin)
      }

      val (provider, server) = createProviderAndServer(service)

      val rolloutCondition = Condition.UuidRange(1 to 20)

      waitFor(100, 100.millis) {
        provider.apply() != DefaultActivations
      }

      provider.close()
      server.shutdownNow()

      val activations = provider.apply()

      activations.apply(toggleOne) mustBe rolloutCondition
      activations.togglesFor("toguru") mustBe Map(toggleOne.id -> rolloutCondition)
    }

    "sends accept header when polling remote url" in {

      var acceptHeader: Option[String] = None
      val service = HttpService {
        case request =>
          acceptHeader = request.headers.get(Accept).map(_.value)
          Ok("""{ "sequenceNo": 10, "toggles": [] }""")
      }

      val (provider, server) = createProviderAndServer(service)

      waitFor(100, 100.millis) {
        acceptHeader.isDefined
      }

      provider.close()
      server.shutdownNow()

      acceptHeader mustBe Some(RemoteActivationsProvider.MimeApiV3)
    }

    "poll remote url with sequenceNo" in {
      val contentTypeV3 = `Content-Type`.parse(RemoteActivationsProvider.MimeApiV3).right.get

      val response =
        Ok(
          """
            |{
            |  "sequenceNo": 10,
            |  "toggles": [{"id":"toggle-one","tags":{"team":"Toguru Team","services":"toguru"},"activations":[{"rollout":{"percentage":20}, "attributes":{}}]}]
            |}""".stripMargin).withContentType(Some(contentTypeV3))

      var maybeSeqNo: Option[Long] = None

      val service = HttpService {
        case request =>
          if(request.uri.params.isDefinedAt("seqNo"))
            maybeSeqNo = Some(request.uri.params("seqNo").toLong)
          response
      }
      val (provider, server) = createProviderAndServer(service)

      waitFor(100, 100.millis) { maybeSeqNo.isDefined }

      provider.close()
      server.shutdownNow()

      maybeSeqNo mustBe Some(10)
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

    success mustBe true
  }

  def freePort: Int = {
    val socket = new ServerSocket(0)
    val port = socket.getLocalPort
    socket.close()
    port
  }
}

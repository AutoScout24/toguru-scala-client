package toguru.impl

import java.util.concurrent.Executors

import net.jodah.failsafe.CircuitBreaker
import org.mockito.Mockito._
import org.mockito.scalatest.IdiomaticMockito
import org.scalatest.OptionValues
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client.testing.SttpBackendStub
import sttp.client.{Identity, Response}
import sttp.model.{Header, MediaType}
import toguru.api.{Condition, DefaultActivations, Toggle}
import toguru.impl.RemoteActivationsProvider.{PollResponse, TogglePoller}

import scala.concurrent.duration.{FiniteDuration, _}

class RemoteActivationsProviderSpec extends AnyWordSpec with OptionValues with Matchers with IdiomaticMockito {

  val toggleOne = Toggle("toggle-one")
  val toggleTwo = Toggle("toggle-two")
  val executor  = Executors.newSingleThreadScheduledExecutor()

  def poller(response: String, contentType: String = RemoteActivationsProvider.MimeApiV3): TogglePoller =
    _ => PollResponse(200, contentType, response)

  def createCircuitBreaker(): CircuitBreaker[Any] =
    new CircuitBreaker[Any]().withFailureThreshold(1000).withDelay(java.time.Duration.ofMillis(100))

  def createProvider(poller: TogglePoller): RemoteActivationsProvider =
    new RemoteActivationsProvider(poller, executor, circuitBreakerBuilder = createCircuitBreaker).close()

  def createProvider(
      response: String,
      contentType: String = RemoteActivationsProvider.MimeApiV3
  ): RemoteActivationsProvider =
    createProvider(poller(response, contentType))

  def createProvider(
      backend: SttpBackendStub[Identity, Nothing]
  ): RemoteActivationsProvider =
    RemoteActivationsProvider(
      s"http://localhost:80",
      pollInterval = 100.milliseconds,
      circuitBreakerBuilder = createCircuitBreaker
    )(backend)

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

    "fail if toggle endpoint returns 500" in {
      val poller: TogglePoller = _ => PollResponse(500, "", "")
      val provider             = createProvider(poller)

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
      val provider             = createProvider(poller)

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

    def toguruResponse(body: String): Response[String] =
      Response
        .ok(body)
        .copy(headers =
          List(
            Header.contentType(
              MediaType
                .parse(RemoteActivationsProvider.MimeApiV3)
                .getOrElse(throw new IllegalArgumentException)
            )
          )
        )

    "poll remote url" in {
      val stub = SttpBackendStub.synchronous.whenAnyRequest.thenRespond(
        toguruResponse(
          """
            |{
            |  "sequenceNo": 10,
            |  "toggles": [{"id":"toggle-one","tags":{"team":"Toguru Team","services":"toguru"},"activations":[{"rollout":{"percentage":20}, "attributes":{}}]}]
            |}
       """.stripMargin
        )
      )

      val provider = createProvider(stub)

      val rolloutCondition = Condition.UuidRange(1 to 20)

      waitFor(100, 100.millis) {
        provider.apply() != DefaultActivations
      }

      provider.close()

      val activations = provider.apply()

      activations.apply(toggleOne) mustBe rolloutCondition
      activations.togglesFor("toguru") mustBe Map(toggleOne.id -> rolloutCondition)
    }

    "sends accept header when polling remote url" in {

      var acceptHeader: Option[String] = None
      val stub =
        SttpBackendStub.synchronous.whenAnyRequest.thenRespondWrapped { req =>
          acceptHeader = req.headers
            .find(_.name == "Accept")
            .map(_.value)
          toguruResponse("""{ "sequenceNo": 10, "toggles": [] }""")
        }
      val provider = createProvider(stub)

      waitFor(100, 100.millis) {
        acceptHeader.isDefined
      }

      provider.close()

      acceptHeader mustBe Some(RemoteActivationsProvider.MimeApiV3)
    }

    "poll remote url with sequenceNo" in {
      var maybeSeqNo: Option[String] = None
      val stub =
        SttpBackendStub.synchronous.whenAnyRequest.thenRespondWrapped { req =>
          maybeSeqNo = req.uri.paramsMap.get("seqNo")
          toguruResponse(
            """
              |{
              |  "sequenceNo": 10,
              |  "toggles": [{"id":"toggle-one","tags":{"team":"Toguru Team","services":"toguru"},"activations":[{"rollout":{"percentage":20}, "attributes":{}}]}]
              |}""".stripMargin
          )
        }

      val provider = createProvider(stub)

      waitFor(100, 100.millis)(maybeSeqNo.isDefined)

      provider.close()

      maybeSeqNo mustBe Some("10")
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
      if (test)
        true
      else {
        if (i < times)
          Thread.sleep(wait.toMillis)
        false
      }
    }

    success mustBe true
  }

}

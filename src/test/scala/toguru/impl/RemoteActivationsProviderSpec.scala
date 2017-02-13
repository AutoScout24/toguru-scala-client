package toguru.impl

import java.net.ServerSocket
import java.util.concurrent.Executors

import org.http4s.headers._
import org.http4s.server.blaze.BlazeBuilder
import org.scalatest.mock.MockitoSugar
import org.mockito.Mockito._
import org.scalatest.{OptionValues, ShouldMatchers, WordSpec}
import toguru.api.{Activations, Condition, DefaultActivations, Toggle}
import toguru.impl.RemoteActivationsProvider.{PollResponse, TogglePoller}

import scala.concurrent.duration._

class RemoteActivationsProviderSpec extends WordSpec with OptionValues with ShouldMatchers with MockitoSugar {

  val toggleOne = Toggle("toggle-one")
  val toggleTwo = Toggle("toggle-two")
  val executor = Executors.newSingleThreadScheduledExecutor()

  def poller(response: String, contentType: String = RemoteActivationsProvider.MimeApiV3): TogglePoller =
    _ => PollResponse(200, contentType, response)

  def createProvider(poller: TogglePoller): RemoteActivationsProvider =
    new RemoteActivationsProvider(poller, executor).close()

  def createProvider(response: String, contentType: String = RemoteActivationsProvider.MimeApiV3): RemoteActivationsProvider =
    createProvider(poller(response, contentType))

  "Fetching features from toggle endpoint" should {

    def validateResponse(toggles: Seq[ToggleState], activations: Activations): Unit = {
      val toggleStateOne = toggles.collectFirst { case t if t.id == toggleOne.id => t }.value
      val toggleStateTwo = toggles.collectFirst { case t if t.id == toggleTwo.id => t }.value

      toggleStateOne.id shouldBe "toggle-one"
      toggleStateOne.tags shouldBe Map("services" -> "toguru")
      toggleStateOne.rollout shouldBe None

      toggleStateTwo.id shouldBe "toggle-two"
      toggleStateTwo.tags shouldBe Map("team" -> "Shared Services")
      toggleStateTwo.rollout.value.percentage shouldBe 20

      activations.apply(toggleOne) shouldBe Condition.Off
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
          |    { "id": "toggle-one", "tags": {"services": "toguru"}},
          |    { "id": "toggle-two", "tags": {"team": "Shared Services"}, "rollout": { "percentage": 20 } }
          |  ]
          |}
          |""".stripMargin

      val provider = createProvider(response)

      val toggles = provider.fetchToggleStates(None).value.toggles
      provider.update()
      val activations = provider.apply()

      validateResponse(toggles, activations)
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

      activations.stateSequenceNo shouldBe Some(10)
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

      maybeToggleStates shouldBe None
    }

    "fails when server returns no sequence number, but client already has one" in {
      val response = """[]"""

      val provider = createProvider(response)

      val maybeToggleStates = provider.fetchToggleStates(Some(10))

      maybeToggleStates shouldBe None
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
      provider.update()
      val activations = provider.apply()

      validateResponse(toggles, activations)
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
      provider.update()
      val activations = provider.apply()

      validateResponse(toggles, activations)
    }

    "fail if toggle endpoint returns 500" in {
      val poller: TogglePoller = _ => PollResponse(500, "", "")
      val provider = createProvider(poller)

      provider.fetchToggleStates(None) shouldBe None

      provider.update()
      provider.apply() shouldBe DefaultActivations
    }

    "fail if toggle endpoint returns malformed json" in {
      val provider = createProvider("ok")

      provider.fetchToggleStates(None) shouldBe None

      provider.update()
      provider.apply() shouldBe DefaultActivations
    }

    "fail if poller throws exception" in {
      val poller: TogglePoller = _ => throw new RuntimeException("boom")
      val provider = createProvider(poller)

      provider.fetchToggleStates(None) shouldBe None
    }
  }

  "Creating activation provider" should {
    "succeed with a valid url" in {
      val provider = RemoteActivationsProvider("http://localhost:9000")
      provider.close()
    }
  }

  "Created activation provider" should {
    import org.http4s._
    import org.http4s.dsl._

    def createProviderAndServer(service: HttpService) = {
      val port = freePort
      (RemoteActivationsProvider(s"http://localhost:$port", pollInterval = 100.milliseconds),
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

      activations.apply(toggleOne) shouldBe rolloutCondition
      activations.togglesFor("toguru") shouldBe Map(toggleOne.id -> rolloutCondition)
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

      acceptHeader shouldBe Some(RemoteActivationsProvider.MimeApiV3)
    }

    "poll remote url with sequenceNo" in {
      val contentTypeV3 = `Content-Type`.parse(RemoteActivationsProvider.MimeApiV3).toOption.get

      val response =
        Ok(
          """
            |{
            |  "sequenceNo": 10,
            |  "toggles": [{"id":"toggle-one","tags":{"team":"Toguru Team","services":"toguru"},"rollout":{"percentage":20}}]
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

      maybeSeqNo shouldBe Some(10)
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

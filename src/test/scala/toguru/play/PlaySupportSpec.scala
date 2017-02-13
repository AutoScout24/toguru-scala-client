package toguru.play

import java.util.UUID
import javax.inject.Inject

import akka.util.Timeout
import org.scalatest.{ShouldMatchers, WordSpec}
import play.api.http.{HeaderNames, HttpVerbs}
import play.api.mvc._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import org.scalatest.OptionValues._
import toguru.api._
import toguru.impl.RemoteActivationsProvider
import toguru.test.TestActivations

import scala.concurrent.duration._
import scala.language.implicitConversions

class PlaySupportSpec extends WordSpec with ShouldMatchers {

  val UserAgent = HeaderNames.USER_AGENT

  val toggle = Toggle("toggle-1")

  val client: PlayClientProvider = { implicit request =>
    import PlaySupport._
    ClientInfo(uuidFromCookieValue("myVisitor"), forcedToggle).withAttribute(fromCookie("culture")).withAttribute(fromHeader(UserAgent))
  }

  // you will write such a class in your play app to automatically convert from Play's RequestHeader to ClientInfo
  abstract class ToggledController(toguru: PlayToguruClient) extends Controller {
    val ToggledAction = PlaySupport.ToggledAction(toguru)
  }

  class MyController @Inject()(toguru: PlayToguruClient) extends ToggledController(toguru) {

    def myAction = ToggledAction { implicit request =>
      if(toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }

  class MyRequest[A](toguru: PlayToguruClient, request : Request[A]) extends WrappedRequest[A](request) with Toggling {
    override val client = toguru.clientProvider(request)

    override val activations = toguru.activationsProvider()
  }

  class MyControllerWithOwnTogglingInfo @Inject()(toguru: PlayToguruClient) extends Controller {

    def myAction = Action { request =>
      implicit val toggling = toguru(request)

      if(toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }

  def createToggledController(provider: Activations.Provider = TestActivations()()) = {

    val toguruClient = PlaySupport.testToguruClient(client, provider)


    new ToggledController(toguruClient) { }
  }

  "toguruClient method" should {
    "create a PlayToguruClient" in {
      val client = PlaySupport.toguruClient(_ => ClientInfo(), "http://localhost:9001")
      client shouldBe a[PlayToguruClient]

      client.activationsProvider.asInstanceOf[RemoteActivationsProvider].close()
    }
  }

  "ToggledAction helper" should {
    "provide request with toggling information" in {
      implicit val timeout = Timeout(2.seconds)
      val toguru = PlaySupport.testToguruClient(client, TestActivations(toggle -> Condition.On)())
      val controller = new MyController(toguru)

      val request = FakeRequest(HttpVerbs.GET, "/")

      val response = controller.myAction(request)

      Helpers.contentAsString(response) shouldBe "Toggle is on"
    }
  }

  "Direct toggling info" should {
    "provide toggling information" in {
      implicit val timeout = Timeout(2.seconds)
      val toguru = PlaySupport.testToguruClient(client, TestActivations(toggle -> Condition.On)())
      val controller = new MyControllerWithOwnTogglingInfo(toguru)

      val request = FakeRequest(HttpVerbs.GET, "/")

      val response = controller.myAction(request)

      Helpers.contentAsString(response) shouldBe "Toggle is on"
    }
  }


  val fakeHeaders = FakeHeaders(Seq(
    UserAgent -> "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
    "X-toguru" -> "feature1-Forced-By-Header=true|feature2-Forced-By-Header=true",
    HeaderNames.COOKIE ->
      Cookies.encodeCookieHeader(Seq(
        Cookie("culture", "de-DE"),
        Cookie("myVisitor", "a5f409eb-2fdd-4499-b65b-b22bd7e51aa2"),
        Cookie("toguru", "feature1-Forced-By-Cookie=true|feature2-Forced-By-Cookie=true")
      ))
  ))

  val request = FakeRequest.apply("GET", "http://priceestimation.autoscout24.de?toguru=feature-forced-by-query-param%3Dtrue", fakeHeaders, "body")
  val requestWithToggleIdUpppercased = FakeRequest.apply("GET", "http://priceestimation.autoscout24.de?toguru=feature-FORCED-by-query-param%3Dtrue", fakeHeaders, "body")
  val requestWithTwoTogglesInQueryString = FakeRequest.apply("GET",
    "http://priceestimation.autoscout24.de?toguru=feature1-forced-by-query-param%3Dtrue%7Cfeature2-forced-by-query-param%3Dfalse", fakeHeaders, "body")

  "Conversion of request header to ClientInfo" should {

    "extract culture attribute" in {
      val clientInfo = client(request)
      clientInfo.attributes.get("culture").value shouldBe "de-DE"
    }

    "extract of user agent from user agent header" in {
      val clientInfo = client(request)
      clientInfo.attributes.get(UserAgent).value shouldBe "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
    }

    "extract of uuid from GUID or visitor cookie" in {
      val clientInfo = client(request)
      clientInfo.uuid.value shouldBe UUID.fromString("a5f409eb-2fdd-4499-b65b-b22bd7e51aa2")
    }
  }

  "Forcing feature toggles" should {
    "override feature toggles by http header" in {
      client(request).forcedToggle("feature1-Forced-By-HEADER").value shouldBe true
      client(request).forcedToggle("feature2-Forced-By-Header").value shouldBe true
    }

    "override feature toggles by cookie" in {
      client(request).forcedToggle("feature1-Forced-By-COOKIE").value shouldBe true
      client(request).forcedToggle("feature2-Forced-By-Cookie").value shouldBe true
    }

    "override one feature toggle by query param" in {
      val clientInfo: ClientInfo = client(request)
      clientInfo.forcedToggle("feature-forced-by-query-param").value shouldBe true
    }

    "override one feature toggle by query param with unusual case" in {
      val clientInfo: ClientInfo = client(requestWithToggleIdUpppercased)
      clientInfo.forcedToggle("feature-forced-by-query-param").value shouldBe true
    }

    "override two feature toggles by query param" in {
      val clientInfo: ClientInfo = client(requestWithTwoTogglesInQueryString)
      clientInfo.forcedToggle("feature1-forced-by-query-param").value shouldBe true
      clientInfo.forcedToggle("feature2-forced-by-query-param").value shouldBe false
    }

    "override one feature toggle twice by query param takes only first occurrence" in {
      val clientInfo: ClientInfo = client(requestWithTwoTogglesInQueryString)
      clientInfo.forcedToggle("feature1-forced-by-query-param").value shouldBe true
    }
  }
}

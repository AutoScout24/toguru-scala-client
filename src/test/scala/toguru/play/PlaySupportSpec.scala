package toguru.play

import java.util.{Locale, UUID}
import javax.inject.Inject

import akka.util.Timeout
import org.scalatest.{ShouldMatchers, WordSpec}
import play.api.http.{HeaderNames, HttpVerbs}
import play.api.mvc._
import play.api.test.{FakeHeaders, FakeRequest, Helpers}
import org.scalatest.OptionValues._
import toguru.api._
import toguru.test.TestActivations

import scala.concurrent.duration._
import scala.language.implicitConversions

class PlaySupportSpec extends WordSpec with ShouldMatchers {

  val toggle = Toggle("toggle-1")

  // you will write such a class in your play app to automatically convert from Play's RequestHeader to ClientInfo
  abstract class ToggledController(activations: Activations.Provider) extends Controller {
    import PlaySupport._

    val client: ClientProvider = { implicit request =>
      ClientInfo(userAgent, localeFromCookieValue("culture"), uuidFromCookieValue("myVisitor"), forcedToggle)
    }

    val ToggledAction = PlaySupport.ToggledAction(client, activations)
  }

  class MyController @Inject()(activations: Activations.Provider) extends ToggledController(activations) {

    def myAction = ToggledAction { implicit request =>
      if(toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }

  class MyControllerWithOwnTogglingInfo @Inject()(activations: Activations.Provider) extends ToggledController(activations) {

    def myAction = Action { request =>
      implicit val toggling = PlaySupport.togglingInfo(request, client, activations)

      if(toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }

  def createToggledController(provider: Activations.Provider = TestActivations()()) = new ToggledController(provider) { }

  "ToggledAction helper" should {
    "provide request with toggling information" in {
      implicit val timeout = Timeout(2.seconds)
      val testActivations = TestActivations(toggle -> Condition.On)()
      val controller = new MyController(testActivations)

      val request = FakeRequest(HttpVerbs.GET, "/")

      val response = controller.myAction(request)

      Helpers.contentAsString(response) shouldBe "Toggle is on"
    }
  }

  "Direct toggling info" should {
    "provide toggling information" in {
      implicit val timeout = Timeout(2.seconds)
      val testActivations = TestActivations(toggle -> Condition.On)()
      val controller = new MyControllerWithOwnTogglingInfo(testActivations)

      val request = FakeRequest(HttpVerbs.GET, "/")

      val response = controller.myAction(request)

      Helpers.contentAsString(response) shouldBe "Toggle is on"
    }
  }


  val fakeHeaders = FakeHeaders(Seq(
    HeaderNames.USER_AGENT -> "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
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

    "Extraction of locale from culture cookie" in {
      val clientInfo = createToggledController().client(request)
      clientInfo.culture.value shouldBe Locale.GERMANY
    }

    "Extraction of user agent from user agent header" in {
      val clientInfo = createToggledController().client(request)
      clientInfo.userAgent.value shouldBe "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
    }

    "Extraction of uuid from GUID or visitor cookie" in {
      val clientInfo = createToggledController().client(request)
      clientInfo.uuid.value shouldBe UUID.fromString("a5f409eb-2fdd-4499-b65b-b22bd7e51aa2")
    }
  }

  "Forcing feature toggles" should {
    "Forcing feature toggles by http header" in {
      createToggledController().client(request).forcedToggle("feature1-Forced-By-HEADER").value shouldBe true
      createToggledController().client(request).forcedToggle("feature2-Forced-By-Header").value shouldBe true
    }

    "Forcing feature toggles by cookie" in {
      createToggledController().client(request).forcedToggle("feature1-Forced-By-COOKIE").value shouldBe true
      createToggledController().client(request).forcedToggle("feature2-Forced-By-Cookie").value shouldBe true
    }

    "Forcing one feature toggle by query param" in {
      val clientInfo: ClientInfo = createToggledController().client(request)
      clientInfo.forcedToggle("feature-forced-by-query-param").value shouldBe true
    }

    "Forcing one feature toggle by query param with unusual case" in {
      val clientInfo: ClientInfo = createToggledController().client(requestWithToggleIdUpppercased)
      clientInfo.forcedToggle("feature-forced-by-query-param").value shouldBe true
    }

    "Forcing two feature toggles by query param" in {
      val clientInfo: ClientInfo = createToggledController().client(requestWithTwoTogglesInQueryString)
      clientInfo.forcedToggle("feature1-forced-by-query-param").value shouldBe true
      clientInfo.forcedToggle("feature2-forced-by-query-param").value shouldBe false
    }

    "Forcing one feature toggle twice by query param takes only first occurence" in {
      val clientInfo: ClientInfo = createToggledController().client(requestWithTwoTogglesInQueryString)
      clientInfo.forcedToggle("feature1-forced-by-query-param").value shouldBe true
    }
  }
}

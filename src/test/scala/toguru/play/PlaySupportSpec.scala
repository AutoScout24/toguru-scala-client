package toguru.play

import java.util.UUID

import akka.util.Timeout
import org.scalatest.OptionValues._
import org.scalatest.{MustMatchers, WordSpec}
import play.api.http.HttpVerbs
import play.api.test.{FakeRequest, Helpers}
import toguru.api._
import toguru.impl.RemoteActivationsProvider
import toguru.test.TestActivations

import scala.concurrent.duration._
import scala.language.implicitConversions

class PlaySupportSpec extends WordSpec with MustMatchers with RequestHelpers with ControllerHelpers {

  "toguruClient method" should {
    "create a PlayToguruClient" in {
      val client = PlaySupport.toguruClient(_ => ClientInfo(), "http://localhost:9001")
      client mustBe a[PlayToguruClient]

      client.activationsProvider.asInstanceOf[RemoteActivationsProvider].close()
    }
  }

  "ToggledAction helper" should {
    "provide request with toggling information" in {
      implicit val timeout = Timeout(2.seconds)
      val toguru           = PlaySupport.testToguruClient(client, TestActivations(toggle -> Condition.On)())
      val controller       = createMyController(toguru)

      val request = FakeRequest(HttpVerbs.GET, "/")

      val response = controller.myAction(request)

      Helpers.contentAsString(response) mustBe "Toggle is on"
    }
  }

  "Direct toggling info" should {
    "provide toggling information" in {
      implicit val timeout = Timeout(2.seconds)
      val toguru           = PlaySupport.testToguruClient(client, TestActivations(toggle -> Condition.On)())
      val controller       = createMyControllerWithOwnTogglingInfo(toguru)

      val request = FakeRequest(HttpVerbs.GET, "/")

      val response = controller.myAction(request)

      Helpers.contentAsString(response) mustBe "Toggle is on"
    }
  }

  "Conversion of request header to ClientInfo" should {

    "extract culture attribute" in {
      val clientInfo = client(request)
      clientInfo.attributes.get("culture").value mustBe "de-DE"
    }

    "extract of user agent from user agent header" in {
      val clientInfo = client(request)
      clientInfo.attributes
        .get(UserAgent)
        .value mustBe "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36"
    }

    "extract of uuid from GUID or visitor cookie" in {
      val clientInfo = client(request)
      clientInfo.uuid.value mustBe UUID.fromString("a5f409eb-2fdd-4499-b65b-b22bd7e51aa2")
    }
  }

  "Forcing feature toggles" should {
    "override feature toggles by http header" in {
      client(request).forcedToggle("feature1-Forced-By-HEADER").value mustBe true
      client(request).forcedToggle("feature2-Forced-By-Header").value mustBe true
    }

    "override feature toggles by cookie" in {
      client(request).forcedToggle("feature1-Forced-By-COOKIE").value mustBe true
      client(request).forcedToggle("feature2-Forced-By-Cookie").value mustBe true
    }

    "override one feature toggle by query param" in {
      val clientInfo: ClientInfo = client(request)
      clientInfo.forcedToggle("feature-forced-by-query-param").value mustBe true
    }

    "override one feature toggle by query param with unusual case" in {
      val clientInfo: ClientInfo = client(requestWithToggleIdUpppercased)
      clientInfo.forcedToggle("feature-forced-by-query-param").value mustBe true
    }

    "override two feature toggles by query param" in {
      val clientInfo: ClientInfo = client(requestWithTwoTogglesInQueryString)
      clientInfo.forcedToggle("feature1-forced-by-query-param").value mustBe true
      clientInfo.forcedToggle("feature2-forced-by-query-param").value mustBe false
    }

    "override one feature toggle twice by query param takes only first occurrence" in {
      val clientInfo: ClientInfo = client(requestWithTwoTogglesInQueryString)
      clientInfo.forcedToggle("feature1-forced-by-query-param").value mustBe true
    }
  }
}

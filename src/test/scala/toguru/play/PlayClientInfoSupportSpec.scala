package toguru.play

import java.util.{UUID, Locale}

import toguru.ClientInfo
import org.scalatest.FeatureSpec
import play.api.http.HeaderNames
import play.api.mvc.{RequestHeader, Cookie, Cookies}
import play.api.test.FakeHeaders
import play.api.test.FakeRequest
import org.scalatest.OptionValues._

import scala.language.implicitConversions

class PlayClientInfoSupportSpec extends FeatureSpec {

  // you will write such a class in your play app to automatically convert from Play's RequestHeader to ClientInfo
  object ControllersSampleSupport {

    implicit def requestToClientInfo(implicit requestHeader: RequestHeader): ClientInfo = {
      import PlayClientInfoSupport._
      ClientInfo(userAgent, localeFromCookieValue("culture"), uuidFromCookieValue("as24Visitor"), forcedFeatureToggle)
    }
  }

  val fakeHeaders = FakeHeaders(Seq(
    HeaderNames.USER_AGENT -> "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
    "X-toguru" -> "feature1-Forced-By-Header=true|feature2-Forced-By-Header=true",
    HeaderNames.COOKIE ->
      Cookies.encodeCookieHeader(Seq(
        Cookie("culture", "de-DE"),
        Cookie("as24Visitor", "a5f409eb-2fdd-4499-b65b-b22bd7e51aa2"),
        Cookie("toguru", "feature1-Forced-By-Cookie=true|feature2-Forced-By-Cookie=true")
      ))
  ))

  val requestHeader = FakeRequest.apply("GET", "http://priceestimation.autoscout24.de?toguru=feature-forced-by-query-param%3Dtrue", fakeHeaders, "body")
  val requestHeaderWithFeatureToggleUnusualCase = FakeRequest.apply("GET", "http://priceestimation.autoscout24.de?toguru=feature-FORCED-by-query-param%3Dtrue", fakeHeaders, "body")
  val requestHeaderWithTwoFeaturesInQueryString = FakeRequest.apply("GET",
    "http://priceestimation.autoscout24.de?toguru=feature1-forced-by-query-param%3Dtrue%7Cfeature2-forced-by-query-param%3Dfalse", fakeHeaders, "body")

  feature("Conversion of request header to ClientInfo") {

    scenario("Extraction of locale from culture cookie") {
      val clientInfo = ControllersSampleSupport.requestToClientInfo(requestHeader)
      assert(clientInfo.culture.value === Locale.GERMANY)
    }

    scenario("Extraction of user agent from user agent header") {
      val clientInfo = ControllersSampleSupport.requestToClientInfo(requestHeader)
      assert(clientInfo.userAgent.value === "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36")
    }

    scenario("Extraction of uuid from GUID or visitor cookie") {
      val clientInfo = ControllersSampleSupport.requestToClientInfo(requestHeader)
      assert(clientInfo.uuid.value === UUID.fromString("a5f409eb-2fdd-4499-b65b-b22bd7e51aa2"))
    }
  }

  feature("Forcing feature toggles") {
    scenario("Forcing feature toggles by http header") {
      assert(ControllersSampleSupport.requestToClientInfo(requestHeader).forcedFeatureToggle("feature1-Forced-By-HEADER").value)
      assert(ControllersSampleSupport.requestToClientInfo(requestHeader).forcedFeatureToggle("feature2-Forced-By-Header").value)
    }

    scenario("Forcing feature toggles by cookie") {
      assert(ControllersSampleSupport.requestToClientInfo(requestHeader).forcedFeatureToggle("feature1-Forced-By-COOKIE").value)
      assert(ControllersSampleSupport.requestToClientInfo(requestHeader).forcedFeatureToggle("feature2-Forced-By-Cookie").value)
    }

    scenario("Forcing one feature toggle by query param") {
      val clientInfo: ClientInfo = ControllersSampleSupport.requestToClientInfo(requestHeader)
      assert(clientInfo.forcedFeatureToggle("feature-forced-by-query-param").value)
    }

    scenario("Forcing one feature toggle by query param with unusual case") {
      val clientInfo: ClientInfo = ControllersSampleSupport.requestToClientInfo(requestHeaderWithFeatureToggleUnusualCase)
      assert(clientInfo.forcedFeatureToggle("feature-forced-by-query-param").value)
    }

    scenario("Forcing two feature toggles by query param") {
      val clientInfo: ClientInfo = ControllersSampleSupport.requestToClientInfo(requestHeaderWithTwoFeaturesInQueryString)
      assert(clientInfo.forcedFeatureToggle("feature1-forced-by-query-param").value)
      assert(clientInfo.forcedFeatureToggle("feature2-forced-by-query-param").value === false)
    }

    scenario("Forcing one feature toggle twice by query param takes only first occurence") {
      val requestHeaderWithTheSameFeatureTwiceInQueryString = FakeRequest.apply("GET",
        "http://priceestimation.autoscout24.de?toguru=feature1-forced-by-query-param%3Dtrue%7Cfeature1-forced-by-query-param%3Dfalse", fakeHeaders, "body")
      val clientInfo: ClientInfo = ControllersSampleSupport.requestToClientInfo(requestHeaderWithTwoFeaturesInQueryString)
      assert(clientInfo.forcedFeatureToggle("feature1-forced-by-query-param").value === true)
    }
  }
}

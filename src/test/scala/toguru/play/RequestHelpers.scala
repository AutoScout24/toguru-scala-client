package toguru.play

import play.api.http.HeaderNames
import play.api.mvc.{Cookie, Cookies}
import play.api.test.{FakeHeaders, FakeRequest}
import toguru.api.ClientInfo

trait RequestHelpers {
  val client: PlayClientProvider = { implicit request =>
    import PlaySupport._
    ClientInfo(uuidFromCookieValue("myVisitor"), forcedToggle)
      .withAttribute(fromCookie("culture"))
      .withAttribute(fromHeader(UserAgent))
  }

  val UserAgent = HeaderNames.USER_AGENT

  val fakeHeaders = FakeHeaders(
    Seq(
      UserAgent -> "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/41.0.2228.0 Safari/537.36",
      "X-toguru" -> "feature1-Forced-By-Header=true|feature2-Forced-By-Header=true",
      HeaderNames.COOKIE ->
        Cookies.encodeCookieHeader(Seq(
          Cookie("culture", "de-DE"),
          Cookie("myVisitor", "a5f409eb-2fdd-4499-b65b-b22bd7e51aa2"),
          Cookie(
            "toguru",
            "feature1-Forced-By-Cookie=true|feature2-Forced-By-Cookie=true")
        ))
    ))

  val request = FakeRequest.apply(
    "GET",
    "http://priceestimation.autoscout24.de?toguru=feature-forced-by-query-param%3Dtrue",
    fakeHeaders,
    "body")
  val requestWithToggleIdUpppercased = FakeRequest.apply(
    "GET",
    "http://priceestimation.autoscout24.de?toguru=feature-FORCED-by-query-param%3Dtrue",
    fakeHeaders,
    "body")
  val requestWithTwoTogglesInQueryString = FakeRequest.apply(
    "GET",
    "http://priceestimation.autoscout24.de?toguru=feature1-forced-by-query-param%3Dtrue%7Cfeature2-forced-by-query-param%3Dfalse",
    fakeHeaders,
    "body"
  )
}

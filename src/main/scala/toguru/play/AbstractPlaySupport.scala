package toguru.play

import java.util.UUID

import play.api.mvc.RequestHeader
import toguru.api.Toggle.ToggleId
import toguru.api.{Activations, ToguruClient}
import toguru.impl.TogglesString.parse

import scala.util.Try

abstract class AbstractPlaySupport {

  /**
    * Creates a new toguru client based on the given client provider.
    *
    * @param clientProvider the client provider to create [[toguru.api.ClientInfo]]s from Play Requests.
    * @param endpointUrl    the toguru server to use, e.g. <code>http://localhost:9000</code>
    * @return
    */
  def toguruClient(clientProvider: PlayClientProvider,
                   endpointUrl: String): PlayToguruClient =
    new ToguruClient(clientProvider, Activations.fromEndpoint(endpointUrl))

  /**
    * Creates a new toguru client with forced test activations.
    *
    * @param clientProvider  the client provider to create [[toguru.api.ClientInfo]]s from Play Requests.
    * @param testActivations the acrt
    * @return
    */
  def testToguruClient(
      clientProvider: PlayClientProvider,
      testActivations: Activations.Provider): PlayToguruClient =
    new ToguruClient(clientProvider, testActivations)

  def uuidFromCookieValue(cookieName: String)(
      implicit requestHeader: RequestHeader): Option[UUID] =
    requestHeader.cookies
      .get(cookieName)
      .flatMap(c => Try(UUID.fromString(c.value)).toOption)

  def fromCookie(name: String)(
      implicit requestHeader: RequestHeader): Option[(String, String)] =
    requestHeader.cookies.get(name).map(name -> _.value)

  def fromHeader(name: String)(
      implicit requestHeader: RequestHeader): Option[(String, String)] =
    requestHeader.headers.get(name).map(name -> _)

  def forcedToggle(toggleId: ToggleId)(
      implicit requestHeader: RequestHeader): Option[Boolean] = {

    def lowerCaseKeys[T](m: Map[String, T]) = m.map {
      case (k, v) => (k.toLowerCase, v)
    }

    val headers = lowerCaseKeys(requestHeader.headers.toSimpleMap)
    lazy val maybeForcedFromHeader = headers
      .get("x-toguru")
      .orElse(headers.get("toguru"))
      .flatMap(togglesString => parse(togglesString)(toggleId))
    lazy val maybeForcedFromCookie = requestHeader.cookies
      .get("toguru")
      .orElse(requestHeader.cookies.get("toguru"))
      .flatMap(cookie => parse(cookie.value)(toggleId))

    lazy val lowerCasedKeysQueryStringMap = lowerCaseKeys(
      requestHeader.queryString)

    lazy val parseToggleQueryString: ToggleId => Option[Boolean] = {
      val maybeToggleString: Option[List[String]] =
        lowerCasedKeysQueryStringMap.get("toguru").map(_.toList)

      toggleId =>
        {
          maybeToggleString
            .map {
              case Nil => None
              case toggleString :: _ => // we ignore toggles defined twice by the client
                parse(toggleString)(toggleId)
            }
            .getOrElse(None)
        }
    }

    val maybeForcedFromQueryParam = parseToggleQueryString(toggleId)
    maybeForcedFromQueryParam
      .orElse(maybeForcedFromHeader)
      .orElse(maybeForcedFromCookie)
  }
}

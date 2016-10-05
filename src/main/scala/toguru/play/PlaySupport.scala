package toguru.play

import java.util.{Locale, UUID}

import play.api.http.HeaderNames
import play.api.i18n.Lang
import play.api.mvc._
import toguru.impl.TogglesString._
import toguru.api.Toggle.ToggleId
import toguru.api._

import scala.language.implicitConversions
import scala.concurrent.Future
import scala.util.Try

/**
  * Default support methods for creating a ClientInfo from a Play Request and for toggling within play actions.
  *
  * If you are fine with the way this object extracts the locale, UUID, and forced feature activation string from the play
  * request header you have to write something like this in your app, and let your controllers extend it while providing
  * the toguru client via injection in your Controller classes. The toguru client can be created with the toguruClient
  * methods of this companion object.
  *
  * {{{
  abstract class ToggledController(toguru: PlayToguruClient) extends Controller {
    val ToggledAction = PlaySupport.ToggledAction(toguru)
  }

  class MyController @Inject()(toguru: PlayToguruClient) extends ToggledController(toguru) {

    def myAction = ToggledAction { (request: ToggledRequest[AnyContent]) =>
      if(toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }
 }}}
  * In case you already have a custom request in your app, consider extending it by applying the [[toguru.api.Toggling]]
  * trait:
  * {{{
  class MyRequest[A](toguru: PlayToguruClient, request : Request[A]) extends WrappedRequest[A](request) with Toggling {
    override val client = toguru.clientProvider(request)

    override val activations = toguru.activationsProvider()
  }
  * }}}
  * You can also create your own Toggling instance if you need to wrap the request on your own:
  * {{{
  class MyControllerWithOwnTogglingInfo @Inject()(toguru: PlayToguruClient) extends Controller {

    def myAction = Action { request =>
      implicit val toggling = toguru(request)

      if(toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }
 }}}
 */
object PlaySupport {

  /**
    * Creates a new toguru client based on the given client provider.
    *
    * @param clientProvider the client provider to create [[toguru.api.ClientInfo]]s from [[play.api.mvc.Request]]s.
    * @param endpointUrl the toguru server to use, e.g. <code>http://localhost:9000</code>
    * @return
    */
  def toguruClient(clientProvider: PlayClientProvider, endpointUrl: String): PlayToguruClient =
    new ToguruClient(clientProvider, Activations.fromEndpoint(endpointUrl))

  /**
    * Creates a new toguru client with forced test activations.
    *
    * @param clientProvider the client provider to create [[toguru.api.ClientInfo]]s from [[play.api.mvc.Request]]s.
    * @param testActivations the acrt
    * @return
    */
  def toguruClient(clientProvider: PlayClientProvider, testActivations: Activations.Provider): PlayToguruClient =
    new ToguruClient(clientProvider, testActivations)

  /**
    * Use this method to create your toggled actions based on a client provider and a toggle activation provider.
    *
    * @see [[toguru.play.PlaySupport$#toguruClient]]
    * @param toguruClient the play toguru client to use
    * @return
    */
  def ToggledAction(toguruClient: PlayToguruClient): ActionBuilder[ToggledRequest] =
    Action andThen new TogglingRefiner(toguruClient)

  def userAgent(implicit requestHeader: RequestHeader) = requestHeader.headers.get(HeaderNames.USER_AGENT)

  def localeFromCookieValue(cookieName: String)(implicit requestHeader: RequestHeader): Option[Locale] = for {
    cultureCookie <- requestHeader.cookies.get(cookieName)
    lang <- Lang.get(cultureCookie.value)
  } yield lang.toLocale

  def uuidFromCookieValue(cookieName: String)(implicit requestHeader: RequestHeader): Option[UUID] =
    requestHeader.cookies.get(cookieName).flatMap(c => Try(UUID.fromString(c.value)).toOption)

  def forcedToggle(toggleId: ToggleId)(implicit requestHeader: RequestHeader): Option[Boolean] = {

    def lowerCaseKeys[T](m: Map[String,T]) = m.map { case (k, v) => (k.toLowerCase, v) }

    val headers = lowerCaseKeys(requestHeader.headers.toSimpleMap)
    lazy val maybeForcedFromHeader = headers.get("x-toguru").orElse(headers.get("toguru")).
      flatMap(togglesString => parse(togglesString)(toggleId))
    lazy val maybeForcedFromCookie = requestHeader.cookies.get("toguru")
      .orElse(requestHeader.cookies.get("toguru"))
      .flatMap(cookie => parse(cookie.value)(toggleId))

    lazy val lowerCasedKeysQueryStringMap = lowerCaseKeys(requestHeader.queryString)

    lazy val parseToggleQueryString: ToggleId => Option[Boolean] = {
      val maybeToggleString: Option[List[String]] = lowerCasedKeysQueryStringMap.get("toguru").map(_.toList)

      toggleId => {
        maybeToggleString.map {
          case Nil => None
          case toggleString :: _ => // we ignore toggles defined twice by the client
            parse(toggleString)(toggleId)
        }.getOrElse(None)
      }
    }

    val maybeForcedFromQueryParam = parseToggleQueryString(toggleId)
    maybeForcedFromQueryParam.orElse(maybeForcedFromHeader).orElse(maybeForcedFromCookie)
  }
}
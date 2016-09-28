package toguru.play

import java.util.{Locale, UUID}

import play.api.http.HeaderNames
import play.api.i18n.Lang
import play.api.mvc._
import toguru.impl.TogglesString._
import toguru.api.Toggle.ToggleId
import toguru.api.{Activations, ClientInfo, Toggling, TogglingInfo}

import scala.language.implicitConversions
import scala.concurrent.Future
import scala.util.Try

/**
 * Default support methods for creating a ClientInfo from a Play Request and for toggling within play actions.
 *
 * If you are fine with the way this object extracts the locale, UUID, and forced feature activation string from the play
 * request header you have to write something like this in your app, and let your controllers extend it while providing the activations provider
 * via injection in your Controller classes. The activations provider can be created via toguru.api.Activations.fromEndpoint
 *
 * {{{
  abstract class ToggedController(activations: Activations.Provider) extends Controller {
    import PlaySupport._

    val client: ClientProvider = { implicit request =>
      ClientInfo(userAgent, localeFromCookieValue("culture"), uuidFromCookieValue("myVisitor"), forcedToggle)
    }

    val ToggledAction = PlaySupport.ToggledAction(client, activations)
  }

  class MyController @Inject()(activations: Activations.Provider) extends ToggedController(activations) {
    val toggle = Toggle("toggle-1")

    def myAction = ToggledAction { implicit request =>
      if(toggle.isOn)
        Ok("toggle on")
      else
        Ok("toggle off")
    }
  }
 }}}
 * You can also create your own Toggling instance if you need to wrap the request on your own:
 * {{{
  class MyControllerWithOwnTogglingInfo @Inject()(activations: Activations.Provider) extends ToggedController(activations) {

    def myAction = Action { request =>
      implicit val toggling = PlaySupport.togglingInfo(request, client, activations)

      if(toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }
 }}}
 */
object PlaySupport {

  type ClientProvider = Request[_] => ClientInfo

  def togglingInfo(request: Request[_], client: ClientProvider, activations: Activations.Provider) =
    TogglingInfo(client(request), activations())

  /**
    * Use this method to create your toggled actions based on a client provider and a toggle activation provider.
    *
    * @see [[toguru.api.Activations]], [[toguru.test.TestActivations]] for activations in tests
    *
    * @param clientProvider
    * @param activationsProvider
    * @return
    */
  def ToggledAction(clientProvider: ClientProvider, activationsProvider: Activations.Provider): ActionBuilder[ToggledRequest] =
    Action andThen TogglingRefiner(clientProvider, activationsProvider)

  class ToggledRequest[A](
           val client: ClientInfo,
           val activations: Activations,
           request : Request[A]) extends WrappedRequest[A](request) with Toggling

  def TogglingRefiner[_](clientProvider: ClientProvider, activationsProvider: Activations.Provider) =
    new ActionRefiner[Request, ToggledRequest] {
      def refine[A](request: Request[A]) = Future.successful {
        Right(new ToggledRequest[A](clientProvider(request), activationsProvider(), request))
      }
    }

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
      flatMap(togglesString => parseForcedTogglesString(togglesString)(toggleId))
    lazy val maybeForcedFromCookie = requestHeader.cookies.get("toguru")
      .orElse(requestHeader.cookies.get("toguru"))
      .flatMap(cookie => parseForcedTogglesString(cookie.value)(toggleId))

    lazy val lowerCasedKeysQueryStringMap = lowerCaseKeys(requestHeader.queryString)

    lazy val parseToggleQueryString: ToggleId => Option[Boolean] = {
      val maybeToggleString: Option[List[String]] = lowerCasedKeysQueryStringMap.get("toguru").map(_.toList)

      toggleId => {
        maybeToggleString.map {
          case Nil => None
          case toggleString :: _ => // we ignore toggles defined twice by the client
            parseForcedTogglesString(toggleString)(toggleId)
        }.getOrElse(None)
      }
    }

    val maybeForcedFromQueryParam = parseToggleQueryString(toggleId)
    maybeForcedFromQueryParam.orElse(maybeForcedFromHeader).orElse(maybeForcedFromCookie)
  }
}
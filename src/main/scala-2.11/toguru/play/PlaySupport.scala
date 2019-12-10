package toguru.play

import java.util.UUID

import play.api.mvc._
import toguru.impl.TogglesString._
import toguru.api.Toggle.ToggleId
import toguru.api._

import scala.language.implicitConversions
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
object PlaySupport extends AbstractPlaySupport {

  /**
    * Use this method to create your toggled actions based on a client provider and a toggle activation provider.
    *
    * @see [[toguru.play.PlaySupport$#toguruClient]]
    * @param toguruClient the play toguru client to use
    * @return
    */
  def ToggledAction(toguruClient: PlayToguruClient): ActionBuilder[ToggledRequest] =
    Action.andThen(new TogglingRefiner(toguruClient))

}

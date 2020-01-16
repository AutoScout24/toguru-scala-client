package toguru.play

import java.util.UUID

import play.api.mvc._
import toguru.impl.TogglesString._
import toguru.api.Toggle.ToggleId
import toguru.api._

import scala.language.implicitConversions
import scala.util.Try

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Default support methods for creating a ClientInfo from a Play Request and for toggling within play actions.
  *
  * If you are fine with the way this object extracts the locale, UUID, and forced feature activation string from the play
  * request header you have to write something like this in your app, and let your controllers extend it while providing
  * the toguru client via injection in your Controller classes. The toguru client can be created with the toguruClient
  * methods of this companion object.
  *
  * {{{
  abstract class ToggledController(toguru: PlayToguruClient, cc: ControllerComponents)
    extends AbstractController(cc) {
    val ToggledAction = PlaySupport.ToggledAction(toguru, cc.parsers.defaultBodyParser)
  }

  class MyController @Inject()(toguru: PlayToguruClient, cc: ControllerComponents)
    extends ToggledController(toguru, cc) {

    def myAction = ToggledAction { implicit request =>
      if (toggle.isOn)
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
  class MyControllerWithOwnTogglingInfo @Inject()(toguru: PlayToguruClient, cc: ControllerComponents)
    extends AbstractController(cc) {

    def myAction = Action { request =>
      implicit val toggling = toguru(request)

      if (toggle.isOn)
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
    * @param bodyParser the bodyParser of the controller action
    * @return
    */
  def ToggledAction(
      toguruClient: PlayToguruClient,
      bodyParser: BodyParser[AnyContent]
  ): ActionBuilder[ToggledRequest, AnyContent] =
    DefaultActionBuilder(bodyParser).andThen(new TogglingRefiner(toguruClient))
}

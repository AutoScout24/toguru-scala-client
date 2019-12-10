package toguru.play

import javax.inject.Inject

import play.api.mvc.{Action, Controller}
import toguru.api.{Activations, Toggle}
import toguru.test.TestActivations

trait ControllerHelpers { this: RequestHelpers =>
  val toggle = Toggle("toggle-1")

  // you will write such a class in your play app to automatically convert from Play's RequestHeader to ClientInfo
  abstract class ToggledController(toguru: PlayToguruClient) extends Controller {
    val ToggledAction = PlaySupport.ToggledAction(toguru)
  }

  class MyController @Inject() (toguru: PlayToguruClient) extends ToggledController(toguru) {

    def myAction = ToggledAction { implicit request =>
      if (toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }

  class MyControllerWithOwnTogglingInfo @Inject() (toguru: PlayToguruClient) extends Controller {

    def myAction = Action { request =>
      implicit val toggling = toguru(request)

      if (toggle.isOn)
        Ok("Toggle is on")
      else
        Ok("Toggle is off")
    }
  }

  def createToggledController(provider: Activations.Provider = TestActivations()()) = {

    val toguruClient = PlaySupport.testToguruClient(client, provider)

    new ToggledController(toguruClient) {}
  }

  def createMyController(toguru: PlayToguruClient) =
    new MyController(toguru)

  def createMyControllerWithOwnTogglingInfo(toguru: PlayToguruClient) =
    new MyControllerWithOwnTogglingInfo(toguru)
}

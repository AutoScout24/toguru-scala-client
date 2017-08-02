package toguru.play

import javax.inject.Inject

import play.api.mvc.Action
import play.api.test.Helpers
import toguru.api.{Activations, Toggle}
import toguru.test.TestActivations
import play.api.mvc._

trait ControllerHelpers {
  this: RequestHelpers =>
  val toggle = Toggle("toggle-1")

  // you will write such a class in your play app to automatically convert from Play's RequestHeader to ClientInfo
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

  def createToggledController(
      provider: Activations.Provider = TestActivations()()) = {

    val toguruClient = PlaySupport.testToguruClient(client, provider)

    new ToggledController(toguruClient, Helpers.stubControllerComponents()) {}
  }

  def createMyController(toguru: PlayToguruClient) = {
    new MyController(toguru, Helpers.stubControllerComponents())
  }

  def createMyControllerWithOwnTogglingInfo(toguru: PlayToguruClient) = {
    new MyControllerWithOwnTogglingInfo(toguru,
                                        Helpers.stubControllerComponents())
  }
}

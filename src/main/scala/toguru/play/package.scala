package toguru

import _root_.play.api.mvc._
import toguru.api.{Activations, ClientInfo, Toggling, ToguruClient}

import scala.concurrent.Future

package object play {

  type PlayToguruClient = ToguruClient[Request[_]]

  type PlayClientProvider = ClientInfo.Provider[Request[_]]

  /**
    * Enriches the request with toggling information. If you have an own enriched class already, consider applying the
    * trait [[toguru.api.Toggling]] to your request class, and implementing it e.g. in this way:
    * {{{
      class MyRequest[A](toguru: PlayToguruClient, request : Request[A]) extends WrappedRequest[A](request) with Toggling {
        override val client = toguru.clientProvider(request)

        override val activations = toguru.activationsProvider()
      }
    * }}}
    *
    * @param toguruClient the toguru client to use.
    */
  class TogglingRefiner(toguruClient: PlayToguruClient) extends ActionRefiner[Request, ToggledRequest] {
    def refine[A](request: Request[A]) = Future.successful {
      Right(new ToggledRequest[A](toguruClient.clientProvider(request), toguruClient.activationsProvider(), request))
    }
  }

  class ToggledRequest[A](
             val client: ClientInfo,
             val activations: Activations,
             request : Request[A]) extends WrappedRequest[A](request) with Toggling
}

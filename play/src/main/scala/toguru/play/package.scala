package toguru

import _root_.play.api.mvc._
import toguru.api.{Activations, ClientInfo, Toggling, ToguruClient}

package object play {

  type PlayToguruClient = ToguruClient[RequestHeader]

  type PlayClientProvider = ClientInfo.Provider[RequestHeader]

  final class ToggledRequest[A](val client: ClientInfo, val activations: Activations, request: Request[A])
      extends WrappedRequest[A](request)
      with Toggling
}

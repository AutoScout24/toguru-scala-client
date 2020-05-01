package toguru.play

import play.api.mvc.{ActionRefiner, Request}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Enriches the request with toggling information. If you have an own enriched class already, consider applying the
  * trait [[toguru.api.Toggling]] to your request class, and implementing it e.g. in this way:
  * {{{
  * class MyRequest[A](toguru: PlayToguruClient, request : Request[A]) extends WrappedRequest[A](request) with Toggling {
  * override val client = toguru.clientProvider(request)
  **
override val activations = toguru.activationsProvider()
  * }
  * }}}
  *
  * @param toguruClient the toguru client to use.
  */
final class TogglingRefiner(toguruClient: PlayToguruClient)(implicit override val executionContext: ExecutionContext)
    extends ActionRefiner[Request, ToggledRequest] {
  def refine[A](request: Request[A]) =
    Future.successful {
      Right(new ToggledRequest[A](toguruClient.clientProvider(request), toguruClient.activationsProvider(), request))
    }
}

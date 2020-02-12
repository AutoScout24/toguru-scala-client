package toguru.api

/**
  * The toguru client containing providers of the data needed for making toggling decisions.
  *
  * @param clientProvider the client provider that extracts client information relevant for toggling.
  * @param activationsProvider the activations provider that returns the current activation state.
  * @tparam T the input type off the client info provider.
  */
final class ToguruClient[T](val clientProvider: ClientInfo.Provider[T], val activationsProvider: Activations.Provider) {

  /**
    * Create a new toggling information instance from the given input
    *
    * @param input the input for creating the client information
    * @return
    */
  def apply(input: T): Toggling = TogglingInfo(clientProvider(input), activationsProvider())

  /**
    * Check whether the toguru system is healthy
    *
    * @return
    */
  def healthy(): Boolean = activationsProvider.healthy()

}

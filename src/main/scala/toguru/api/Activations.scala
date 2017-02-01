package toguru.api

import toguru.api.Toggle.ToggleId
import toguru.impl.RemoteActivationsProvider

object Activations {

  trait Provider {
    def apply(): Activations

    def healthy(): Boolean
  }

  /**
    * Creates an activation provider that polls the given endpoint url to retrieve a toggle state json.
    *
    * @see [[https://github.com/AutoScout24/toguru/blob/master/conf/routes]]
    * @param endpoint the endpoint url to use, e.g. <code>http://localhost:9000/togglestate</code>
    * @return
    */
  def fromEndpoint(endpoint: String): Activations.Provider = RemoteActivationsProvider(endpoint)

}

trait Activations {

  def apply(toggle: Toggle): Condition

  def togglesFor(service: String): Map[ToggleId, Condition]

  def stateSequenceNo: Option[Long]

}

object DefaultActivations extends Activations {

  override def apply(toggle: Toggle) = toggle.default

  override def togglesFor(service: String) = Map.empty

  override def stateSequenceNo: Option[Long] = None
}

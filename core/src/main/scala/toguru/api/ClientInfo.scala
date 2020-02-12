package toguru.api

import java.util.UUID

import toguru.api.Toggle.ToggleId

final case class ClientInfo(
    uuid: Option[UUID] = None,
    forcedToggle: ToggleId => Option[Boolean] = (_) => None,
    attributes: Map[String, String] = Map.empty
) {

  /**
    * create a copy of this client info that is enriched with the given attribute name/value pair
    *
    * @param name attribute name
    * @param value attribute value
    * @return extended client info
    */
  def withAttribute(name: String, value: String): ClientInfo = copy(attributes = attributes.updated(name, value))

  /**
    * optionally creates an enriched version of this client info with the new attribute
    *
    * Note: this is a utility method for integrating with e.g. Toguru PlaySupport.
    *
    * @param attribute an optional attribute
    * @return extended client info if option is defined, and <code>this</code> otherwise
    */
  def withAttribute(attribute: Option[(String, String)]): ClientInfo =
    attribute.map({ case (name, value) => withAttribute(name, value) }).getOrElse(this)

}

object ClientInfo {

  type Provider[T] = T => ClientInfo

}

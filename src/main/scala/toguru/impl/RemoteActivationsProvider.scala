package toguru.impl

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.hootsuite.circuitbreaker.CircuitBreakerBuilder
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.json.Json
import toguru.api.{Activations, Condition, DefaultActivations, Toggle}
import toguru.impl.RemoteActivationsProvider._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

object RemoteActivationsProvider {

  type TogglePoller = () => (Int, String)

  val executor = Executors.newScheduledThreadPool(1)
  sys.addShutdownHook(executor.shutdownNow())

  private val circuitBreaker = CircuitBreakerBuilder(
    name = "toguru-server-breaker",
    failLimit  = 5,
    retryDelay = FiniteDuration(20, TimeUnit.SECONDS)
  ).build()

  /**
    * Create an activation provider that fetches the toggle activations conditions from the toguru server given.
    *
    * @param endpointUrl the endpoint of the toguru server, e.g. <code>http://localhost:9000</code>
    * @param pollInterval
    * @return
    */
  def apply(endpointUrl: String, pollInterval: Duration = 2.seconds): RemoteActivationsProvider = {
    val poller: TogglePoller = () => {
      val response = Http(endpointUrl + "/togglestate").timeout(500, 750).asString
      (response.code, response.body)
    }

    new RemoteActivationsProvider(poller, executor, pollInterval)
  }

}

/**
  * Fetches the toggle activation conditions from a toguru server.
  *
  * @param poller
  * @param executor
  * @param pollInterval
  */
class RemoteActivationsProvider(poller: TogglePoller, executor: ScheduledExecutorService, pollInterval: Duration = 2.seconds) extends (() => Activations) with StrictLogging {

  val schedule = executor.scheduleAtFixedRate(new Runnable() {
    def run(): Unit = update()
  }, pollInterval.toMillis, pollInterval.toMillis, TimeUnit.MILLISECONDS)

  val currentActivation = new AtomicReference[Activations](DefaultActivations)

  def update() = fetchToggleStates().foreach(ts => currentActivation.set(new ToggleStateActivations(ts)))

  def close(): RemoteActivationsProvider = {
    schedule.cancel(true)
    this
  }

  implicit val toggleReads = Json.reads[ToggleState]

  def fetchToggleStates(): Option[Seq[ToggleState]] = {
    Try(circuitBreaker() { poller() }) match {
      case Success((code, body)) =>
        val tryToggleStates = Try(Json.parse(body).as[Seq[ToggleState]])
        (code, tryToggleStates) match {
          case (200, Success(toggleStates)) =>
            Some(toggleStates)

          case _ =>
            logger.warn(s"Polling registry failed, got response code $code and body '$body'")
            None
        }
      case Failure(e) =>
        logger.warn(s"Polling registry failed", e)
        None
    }
  }

  override def apply() = currentActivation.get()

}

class ToggleStateActivations(toggleStates: Seq[ToggleState]) extends Activations {

  def activationConditions(toggleState: ToggleState): (String, Condition) = {
    val condition = toggleState.rolloutPercentage match {
      case Some(p) => Condition.UuidRange(1 to p)
      case None    => Condition.Off
    }

    toggleState.id -> condition
  }

  val conditions = toggleStates.map(activationConditions).toMap

  override def apply(toggle: Toggle) = conditions.getOrElse(toggle.id, toggle.default)

  override def togglesFor(service: String) = {
    val toggles =
      for {
        toggle <- toggleStates if toggle.tags.get("services").contains(service)
        id = toggle.id
      } yield (id, conditions(id))
    toggles.toMap
  }
}


case class ToggleState(id: String, tags: Map[String, String], rolloutPercentage: Option[Int])
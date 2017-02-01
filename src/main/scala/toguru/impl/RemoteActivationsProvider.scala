package toguru.impl

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, ScheduledExecutorService, TimeUnit}

import com.hootsuite.circuitbreaker.CircuitBreakerBuilder
import com.typesafe.scalalogging.StrictLogging
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import toguru.api.{Activations, Condition, DefaultActivations, Toggle}
import toguru.impl.RemoteActivationsProvider._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scalaj.http.Http

object RemoteActivationsProvider {

  val MimeApiV2 = "application/vnd.toguru.v2+json"

  type TogglePoller = (Option[Long]) => (Int, String)

  implicit val toggleStatesReads: Reads[ToggleStates] = {
    implicit val toggleStateReads = Json.reads[ToggleState]
    val toggleStatesV1Reads = Reads.seq[ToggleState].map(ts => ToggleStates(None, ts))
    Json.reads[ToggleStates] or toggleStatesV1Reads
  }

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
    * @param pollInterval the poll interval to use for querying the toguru server
    * @return
    */
  def apply(endpointUrl: String, pollInterval: Duration = 2.seconds): RemoteActivationsProvider = {
    val poller: TogglePoller = { maybeSeqNo =>
      val maybeSeqNoParam = maybeSeqNo.map(seqNo => s"?seqNo=$seqNo").mkString
      val response = Http(endpointUrl + s"/togglestate$maybeSeqNoParam").header("Accept", MimeApiV2).timeout(500, 750).asString
      (response.code, response.body)
    }

    new RemoteActivationsProvider(poller, executor, pollInterval)
  }
}

/**
  * Fetches the toggle activation conditions from a toguru server.
  *
  * @param poller the poller to fetch toggle states
  * @param executor the executor service for scheduling polling
  * @param pollInterval the polling interval
  */
class RemoteActivationsProvider(poller: TogglePoller, executor: ScheduledExecutorService, val pollInterval: Duration = 2.seconds) extends Activations.Provider
  with ToguruClientMetrics with StrictLogging {

  val schedule = executor.scheduleAtFixedRate(new Runnable() {
    def run(): Unit = update()
  }, pollInterval.toMillis, pollInterval.toMillis, TimeUnit.MILLISECONDS)

  sys.addShutdownHook { close() }

  val currentActivation = new AtomicReference[Activations](DefaultActivations)

  def update() = {
    val sequenceNo = currentActivation.get().stateSequenceNo
    fetchToggleStates(sequenceNo).foreach(ts => currentActivation.set(new ToggleStateActivations(ts)))
  }

  def close(): RemoteActivationsProvider = {
    schedule.cancel(true)
    deregister()
    this
  }


  def fetchToggleStates(sequenceNo: Option[Long]): Option[ToggleStates] = {

    def sequenceNoValid(toggleStates: ToggleStates) = (sequenceNo, toggleStates.sequenceNo) match {
      case (None, _) => true
      case (Some(a), Some(b)) => a <= b
      case (Some(_), None) => false
    }

    Try(circuitBreaker() { poller(sequenceNo) }) match {
      case Success((code, body)) =>
        val tryToggleStates = Try(Json.parse(body).as[ToggleStates])
        (code, tryToggleStates) match {
          case (200, Success(toggleStates)) if sequenceNoValid(toggleStates) =>
            fetchSuccess()
            Some(toggleStates)

          case (200, Success(toggleStates)) =>
            logger.warn(s"Server response contains stale state (sequenceNo. is '${toggleStates.sequenceNo.mkString}'), client sequenceNo is '${sequenceNo.mkString}'.")
            fetchFailed()
            None

          case _ =>
            logger.warn(s"Polling registry failed, got response code $code and body '$body'")
            fetchFailed()
            None
        }
      case Failure(e) =>
        logger.warn(s"Polling registry failed", e)
        connectError()
        None
    }
  }

  override def apply() = currentActivation.get()

  override def currentSequenceNo: Option[Long] = apply().stateSequenceNo
}

class ToggleStateActivations(toggleStates: ToggleStates) extends Activations {

  def activationConditions(toggleState: ToggleState): (String, Condition) = {
    val condition = toggleState.rolloutPercentage match {
      case Some(p) => Condition.UuidRange(1 to p)
      case None    => Condition.Off
    }

    toggleState.id -> condition
  }

  val conditions = toggleStates.toggles.map(activationConditions).toMap

  override def apply(toggle: Toggle) = conditions.getOrElse(toggle.id, toggle.default)

  override def togglesFor(service: String) = {
    val toggles =
      for {
        toggle <- toggleStates.toggles if toggle.tags.get("services").contains(service)
        id = toggle.id
      } yield (id, conditions(id))
    toggles.toMap
  }

  override def stateSequenceNo: Option[Long] = toggleStates.sequenceNo
}


case class ToggleState(id: String, tags: Map[String, String], rolloutPercentage: Option[Int])

case class ToggleStates(sequenceNo: Option[Long], toggles: Seq[ToggleState])
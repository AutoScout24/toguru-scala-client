package toguru.impl

import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.{Executors, ScheduledExecutorService, ScheduledFuture, TimeUnit}

import net.jodah.failsafe.CircuitBreaker
import org.komamitsu.failuredetector.PhiAccuralFailureDetector
import org.slf4j.LoggerFactory
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import sttp.client._
import sttp.model.Header
import toguru.api.{Activations, DefaultActivations}
import toguru.impl.RemoteActivationsProvider._

import scala.concurrent.duration._
import scala.util.control.NonFatal
import scala.util.{Success, Try}

object RemoteActivationsProvider {

  val MimeApiV3 = "application/vnd.toguru.v3+json"

  case class PollResponse(code: Int, contentType: String, content: String)

  type TogglePoller = (Option[Long]) => PollResponse

  val toggleStateReadsUntilV2: Reads[ToggleStates] = {
    val toggleStateV1Reads = (JsPath \ "id")
      .read[String]
      .and((JsPath \ "rolloutPercentage").readNullable[Int])
      .and((JsPath \ "tags").read[Map[String, String]])((id, p, tags) =>
        ToggleState(id, tags, Seq(ToggleActivation(p.map(Rollout))))
      )

    val toggleStatesV1Reads = Reads.seq(toggleStateV1Reads).map(ts => ToggleStates(None, ts))

    val toggleStatesV2Reads = (JsPath \ "sequenceNo")
      .read[Int]
      .and((JsPath \ "toggles").read(Reads.list(toggleStateV1Reads)))((seqNo, toggles) =>
        ToggleStates(Some(seqNo), toggles)
      )

    toggleStatesV2Reads.or(toggleStatesV1Reads)
  }

  val toggleStateReads = {
    implicit val rolloutReads    = Json.reads[Rollout]
    implicit val activationReads = Json.reads[ToggleActivation]

    implicit val toggleStateReads = (JsPath \ "id")
      .read[String]
      .and((JsPath \ "activations").read[Seq[ToggleActivation]])
      .and((JsPath \ "tags").read[Map[String, String]])((id, acts, tags) => ToggleState(id, tags, acts))

    Json.reads[ToggleStates]
  }

  val executor = Executors.newScheduledThreadPool(1)
  sys.addShutdownHook(executor.shutdownNow())

  private def createCircuitBreaker(): CircuitBreaker[Any] =
    new CircuitBreaker[Any]()
      .withFailureThreshold(5)
      .withDelay(java.time.Duration.ofSeconds(20))

  /**
    * Create an activation provider that fetches the toggle activations conditions from the toguru server given.
    *
    * @param endpointUrl the endpoint of the toguru server, e.g. <code>http://localhost:9000</code>
    * @param pollInterval the poll interval to use for querying the toguru server
    * @return
    */
  def apply(
      endpointUrl: String,
      pollInterval: Duration = 2.seconds,
      circuitBreakerBuilder: () => CircuitBreaker[Any] = createCircuitBreaker
  )(
      implicit sttpBackend: SttpBackend[Identity, Nothing, NothingT] = HttpURLConnectionBackend()
  ): RemoteActivationsProvider = {
    val poller: TogglePoller = { maybeSeqNo =>
      val requestUri = maybeSeqNo match {
        case Some(seqNo) => uri"$endpointUrl/togglestate?seqNo=$seqNo"
        case None        => uri"$endpointUrl/togglestate"
      }
      val response = quickRequest
        .headers(Header.accept(MimeApiV3))
        .readTimeout(750.millis)
        .get(requestUri)
        .send()
      PollResponse(response.code.code, response.contentType.getOrElse(""), response.body)
    }
    new RemoteActivationsProvider(poller, executor, pollInterval, circuitBreakerBuilder)
  }
}

/**
  * Fetches the toggle activation conditions from a toguru server.
  *
  * @param poller the poller to fetch toggle states
  * @param executor the executor service for scheduling polling
  * @param pollInterval the polling interval
  * @param circuitBreakerBuilder the circuit breaker builder to use for creating the circuit breaker.
  */
class RemoteActivationsProvider(
    poller: TogglePoller,
    executor: ScheduledExecutorService,
    val pollInterval: Duration = 2.seconds,
    circuitBreakerBuilder: () => CircuitBreaker[Any] = RemoteActivationsProvider.createCircuitBreaker
) extends Activations.Provider {

  private val logger = LoggerFactory.getLogger(getClass)

  val circuitBreaker: CircuitBreaker[Any] = circuitBreakerBuilder()

  val connectivity: PhiAccuralFailureDetector = {
    val builder = new PhiAccuralFailureDetector.Builder()

    builder.setThreshold(16)
    builder.setMaxSampleSize(200)
    builder.setAcceptableHeartbeatPauseMillis(pollInterval.toMillis)
    builder.setFirstHeartbeatEstimateMillis(pollInterval.toMillis)
    builder.setMinStdDeviationMillis(100)

    builder.build()
  }

  // this heartbeat call is needed to kickoff phi computation
  connectivity.heartbeat()

  val schedule: ScheduledFuture[_] = executor.scheduleAtFixedRate(new Runnable() {
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
    this
  }

  def fetchToggleStates(sequenceNo: Option[Long]): Option[ToggleStates] = {

    def sequenceNoValid(toggleStates: ToggleStates) = (sequenceNo, toggleStates.sequenceNo) match {
      case (None, _)          => true
      case (Some(a), Some(b)) => a <= b
      case (Some(_), None)    => false
    }

    def parseBody(response: PollResponse): Try[ToggleStates] = {
      val reads = response.contentType match {
        case MimeApiV3 => toggleStateReads
        case _         => toggleStateReadsUntilV2
      }

      Try(Json.parse(response.content).as(reads))
    }

    if (circuitBreaker.allowsExecution()) {
      try {
        circuitBreaker.preExecute()
        val pollResponse    = poller(sequenceNo)
        val tryToggleStates = parseBody(pollResponse)
        (pollResponse.code, tryToggleStates) match {
          case (200, Success(toggleStates)) if sequenceNoValid(toggleStates) =>
            circuitBreaker.recordSuccess()
            fetchSuccess()
            Some(toggleStates)
          case (200, Success(toggleStates)) =>
            circuitBreaker.recordSuccess()
            if (logger.isWarnEnabled) {
              logger.warn(
                s"Server response contains stale state (sequenceNo. is '${toggleStates.sequenceNo.mkString}'), client sequenceNo is '${sequenceNo.mkString}'."
              )
            }
            None
          case _ =>
            circuitBreaker.recordFailure()
            if (logger.isWarnEnabled) {
              logger.warn(
                s"Polling registry failed, got response code ${pollResponse.code} and body '${pollResponse.content}'"
              )
            }
            None
        }
      } catch {
        case NonFatal(e) =>
          circuitBreaker.recordFailure(e)
          if (logger.isWarnEnabled) {
            logger.warn(s"Polling registry failed (${e.getClass.getName}: ${e.getMessage})")
          }
          None
      }
    } else None
  }

  def fetchSuccess(): Unit = connectivity.heartbeat()

  def healthy(): Boolean = connectivity.isAvailable()

  override def apply(): Activations = currentActivation.get()
}

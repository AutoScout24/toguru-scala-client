package toguru.impl

import com.codahale.metrics.{Gauge, JmxReporter, MetricRegistry}
import org.komamitsu.failuredetector.PhiAccuralFailureDetector

import scala.concurrent.duration.Duration

trait ToguruClientMetrics {

  def pollInterval: Duration

  def currentSequenceNo: Option[Long]

  val RegistryDomain = "toguru-client"

  val ConnectivityPhiGauge = "connectivity-phi-gauge"
  val SequenceNoGauge      = "sequence-no-gauge"
  val ConnectErrorCount    = "connect-error-count"
  val FetchFailureCount    = "fetch-failure-count"

  val metricsRegistry = new MetricRegistry()

  val reporter = JmxReporter.forRegistry(metricsRegistry).inDomain(RegistryDomain).build()

  reporter.start()

  val connectivity = {
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

  metricsRegistry.register(ConnectivityPhiGauge, new Gauge[Double] { def getValue = connectivity.phi() })

  metricsRegistry.register(SequenceNoGauge, new Gauge[Long] { def getValue: Long = currentSequenceNo.getOrElse(0) })

  val connectErrors = metricsRegistry.counter(ConnectErrorCount)

  val fetchFailures = metricsRegistry.counter(FetchFailureCount)

  def fetchSuccess() = connectivity.heartbeat()

  def fetchFailed()  = fetchFailures.inc()

  def connectError() = connectErrors.inc()

  def healthy() = connectivity.isAvailable()

  def deregister() = {
    metricsRegistry.remove(ConnectivityPhiGauge)
    metricsRegistry.remove(ConnectErrorCount)
    metricsRegistry.remove(FetchFailureCount)
    reporter.close()
  }
}

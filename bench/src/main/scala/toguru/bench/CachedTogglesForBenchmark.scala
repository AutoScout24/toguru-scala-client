package toguru.bench

import org.openjdk.jmh.annotations
import org.openjdk.jmh.annotations.{Benchmark, BenchmarkMode, Fork, Measurement, Mode, Param, Scope, Setup, Warmup}
import toguru.api.{ClientInfo, Condition, TogglingInfo}
import toguru.impl.{ToggleState, ToggleStateActivations, ToggleStateActivationsWithCaching, ToggleStates}
import toguru.test.TestActivations

import scala.collection.immutable.Seq
import scala.util.Random

object CachedTogglesForBenchmark {

  val services600Toggles10: ToggleStates = ToggleStates(sequenceNo = None, toggles = makeToggles(getServices(600), 10))

  def getServices(numServices: Int): Seq[String] = (1 to numServices).map(_ => Random.alphanumeric.take(10).mkString)

  def makeToggles(services: Seq[String], togglesPerService: Int): Seq[ToggleState] =
    services
      .flatMap { service =>
        (1 to togglesPerService).map(f =
          _ =>
            ToggleState(
              id = Random.alphanumeric.take(10).mkString,
              tags = Map(("service" -> service)),
              condition = Condition.On
            )
        )
      }

  @annotations.State(Scope.Benchmark)
  class State {
    @Param(Array("10", "100", "1000"))
    var togglesNum: Int = 0

    var togglesStates: ToggleStates = ToggleStates(None, Seq.empty[ToggleState])

    var togglingWithCaching: TogglingInfo = TogglingInfo(client = ClientInfo(), TestActivations()()())
    var togglingNoCaching: TogglingInfo   = TogglingInfo(client = ClientInfo(), TestActivations()()())
    var indexes: Seq[Int]                 = Seq()

    @Setup
    def setUp(): Unit = {
      togglesStates = ToggleStates(sequenceNo = None, toggles = makeToggles(getServices(togglesNum), 30))
      togglingWithCaching = TogglingInfo(client = ClientInfo(), new ToggleStateActivationsWithCaching(togglesStates))
      togglingNoCaching = TogglingInfo(client = ClientInfo(), new ToggleStateActivations(togglesStates))
      indexes = Seq(Random.nextInt(togglesNum), Random.nextInt(togglesNum), Random.nextInt(togglesNum))
    }
  }

}

class CachedTogglesForBenchmark {
  import toguru.bench.CachedTogglesForBenchmark._

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 1)
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @Measurement(iterations = 3)
  def toggleActivationsWithCachingEntireLength(state: State): String =
    state.togglingWithCaching
      .toggleStringForService(state.togglesStates.toggles(Random.nextInt(state.togglesStates.toggles.length)).id)

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 1)
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @Measurement(iterations = 3)
  def toggleActivationsNoCachingEntireLength(state: State): String =
    state.togglingNoCaching
      .toggleStringForService(state.togglesStates.toggles(Random.nextInt(state.togglesStates.toggles.length)).id)

  // This is the more likely scenario in which we use a few toggles
  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 1)
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @Measurement(iterations = 3)
  def toggleActivationsWithCachingFew(state: State): String =
    state.togglingWithCaching
      .toggleStringForService(state.togglesStates.toggles(Random.nextInt(state.indexes.length)).id)

  @Fork(value = 1, warmups = 1)
  @Warmup(iterations = 1)
  @Benchmark
  @BenchmarkMode(Array(Mode.Throughput))
  @Measurement(iterations = 3)
  def toggleActivationsNoCachingFew(state: State): String =
    state.togglingNoCaching
      .toggleStringForService(state.togglesStates.toggles(Random.nextInt(state.indexes.length)).id)

}

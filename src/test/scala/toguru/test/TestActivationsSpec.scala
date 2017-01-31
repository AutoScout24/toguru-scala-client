package toguru.test

import org.scalatest._
import toguru.api._

class TestActivationsSpec extends WordSpec with ShouldMatchers {

  "healthy" should {
    "always return true" in {
      TestActivations()().healthy() shouldBe true
    }
  }

  "activations" should {
    "return the activations given in the constructor" in {
      val toggle = Toggle("test-toggle")
      val activationsProvider = TestActivations(toggle -> Condition.On)()
      val activations = activationsProvider()

      activations(toggle) shouldBe Condition.On
    }

    "return the default activations if no activation was given" in {
      val toggle = Toggle("test-toggle", default = Condition.On)
      val activationsProvider = TestActivations()()
      val activations = activationsProvider()

      activations(toggle) shouldBe Condition.On
    }
  }

  "serviceFor" should {
    "return the toggles as given in the test" in {
      val toggle = Toggle("test-toggle")
      val anotherToggle = Toggle("another-toggle")
      val activations = TestActivations(toggle -> Condition.On, anotherToggle -> Condition.Off)(toggle -> "my-service")

      val toggles = activations().togglesFor("my-service")

      toggles.size shouldBe 1
      toggles.get("test-toggle") shouldBe Some(Condition.On)
    }
  }
}

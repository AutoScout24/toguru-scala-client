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

  "togglesFor" should {
    "return the toggles as given in the test" in {
      val toggle = Toggle("test-toggle")
      val anotherToggle = Toggle("another-toggle")
      val activations = TestActivations(toggle -> Condition.On, anotherToggle -> Condition.Off)(toggle -> "my-service")

      val toggles = activations().togglesFor("my-service")

      toggles.size shouldBe 1
      toggles.get("test-toggle") shouldBe Some(Condition.On)
    }
  }

  "togglesForFilter" should {
    "return the toggles as given in the test" in {
      val toggle = Toggle("test-toggle")
      val anotherToggle = Toggle("another-toggle")
      val thirdToggle = Toggle("third-toggle")
      val activations = TestActivationsWithTags(toggle -> Condition.On, anotherToggle -> Condition.Off, thirdToggle -> Condition.On)(
        toggle -> Map("tag1" -> "A", "tag2" -> "B"),
        anotherToggle -> Map("tag1" -> "A", "tag2" -> "A"),
        thirdToggle -> Map("tag1" -> "B", "tag2" -> "B"))

      val toggles = activations().togglesForFilter { case (tags, condition) => tags.get("tag1").contains("A") && tags.get("tag2").contains("A") }

      toggles.size shouldBe 1
      toggles.get("another-toggle") shouldBe Some(Condition.Off)

      val toggles2 = activations().togglesForFilter { case (tags, condition) => condition == Condition.Off }

      toggles2.size shouldBe 1
      toggles2.get("another-toggle") shouldBe Some(Condition.Off)
    }
  }

}

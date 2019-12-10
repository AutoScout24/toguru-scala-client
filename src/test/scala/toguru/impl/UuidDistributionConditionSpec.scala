package toguru.impl

import java.util.UUID

import org.scalatest.prop.TableDrivenPropertyChecks
import org.scalatest.{MustMatchers, WordSpec}
import toguru.api.ClientInfo

class UuidDistributionConditionSpec extends WordSpec with MustMatchers with TableDrivenPropertyChecks {

  "Invalid ranges" should {
    "Lower boundary is too low" in {

      intercept[IllegalArgumentException] {
        UuidDistributionCondition.apply(0 to 10)
      }.getMessage mustBe "Range should describe a range between 1 and 100 inclusive"
    }

    "Upper boundary is too high" in {
      intercept[IllegalArgumentException] {
        UuidDistributionCondition.apply(90 to 101)
      }.getMessage mustBe "Range should describe a range between 1 and 100 inclusive"
    }
  }

  "Valid UUIDs" should {
    "be projected within max range" in {
      (1 to 1000).foreach { _ =>
        val uuid = UUID.randomUUID()
        val info = ClientInfo(uuid = Some(uuid))
        UuidDistributionCondition.apply(1 to 100).applies(info) mustBe true
      }
    }
  }

  "defaultUuidToIntProjection" should {
    val uuidMappings = Table(
      ("uuid", "bucket"),
      ("92cccc65-3ee8-4fa8-a663-632dd282f42f", 1),
      ("ef4ff1f7-d0d6-4129-b823-818e39b8dc24", 6),
      ("5f908fdb-e569-4a2e-973f-73fe51b8a337", 11),
      ("2cce917e-db13-4c16-8358-fb2c57069391", 11),
      ("88248687-6dce-4759-a5c0-3945eedc2b48", 22),
      ("0f27a764-029a-4716-8f1f-13b83fd53ff7", 23),
      ("4bc77248-20fd-4946-bf09-183cabd34139", 31),
      ("8f087b84-d989-4db9-8082-bd566ae9da56", 38),
      ("363f325b-a1ea-488a-bfef-ad0f6578069c", 39),
      ("d35bdaeb-ec34-474e-8536-c228120806d2", 55),
      ("bd09284d-3489-4a2f-8fbf-1639aaf5b570", 56),
      ("288e4fb2-97c0-4be7-b831-dd33156bb4ef", 60),
      ("54c83aaf-7ca0-4e8a-8ca7-c0d97b23f0c8", 71),
      ("721f87e2-cec9-4753-b3bb-d2ebe20dd317", 76),
      ("f36d7579-77b2-47e9-859a-6e92156e430b", 78),
      ("f35cb052-0e75-4393-ba2f-84aaab15f087", 80),
      ("6eb1698e-1a9e-4fc9-be24-36f68021b335", 82),
      ("86bf949b-a75b-44b2-8e66-2f766fa43f48", 91),
      ("476893d9-65cd-410d-a294-ac086c5fffa3", 94),
      ("88d8bbd2-809f-4edf-a38e-7a98dddc06a3", 96)
    )

    "map UUIDs consistently" in {

      val projection = UuidDistributionCondition.defaultUuidToIntProjection

      forAll(uuidMappings) { (uuid: String, bucket: Int) =>
        projection(UUID.fromString(uuid)) mustBe bucket
      }
    }
  }
}

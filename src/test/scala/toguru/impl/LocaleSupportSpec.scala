package toguru.impl

import java.util.Locale

import org.scalatest.WordSpec

class LocaleSupportSpec extends WordSpec {

  import LocaleSupport.RichLocale

  "Country extraction from locale" should {
    "Country gets successfully extracted" in {
      assert(Locale.GERMANY.country === Some("DE"))
    }

    "Unset country returns null for extraction" in {
      assert(new Locale("", "").country === None)
    }
  }

  "Lang extraction from locale" should  {
    "extract language" in {
      assert(Locale.GERMANY.lang === Some("de"))
    }

    "return null for extraction if unset" in {
      assert(new Locale("", "").lang === None)
    }
  }

}

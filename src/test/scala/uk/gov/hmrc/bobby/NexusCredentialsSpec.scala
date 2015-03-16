package uk.gov.hmrc.bobby

import java.net.URL

import org.scalatest.{FlatSpec, Matchers}

import scala.util.Try

class NexusCredentialsSpec extends FlatSpec with Matchers {

  "buildsearchUrl" should
    "build valid URL" in {
      val result = NexusCredentials("nexus.host.com", "myUsername", "somePassword").buildSearchUrl("query")
      result shouldBe "https://myUsername:somePassword@nexus.host.com/service/local/lucene/search?a=query"
      Try(new URL(result)).toOption shouldBe defined
    }

    it should "encode characters that may break the URL" in {
    val result = NexusCredentials("nexus.host.com", "myUsername@mydomain.com", "someP!ssword").buildSearchUrl("query")
    result shouldBe "https://myUsername%40mydomain.com:someP%21ssword@nexus.host.com/service/local/lucene/search?a=query"
    Try(new URL(result)).toOption shouldBe defined
  }


}

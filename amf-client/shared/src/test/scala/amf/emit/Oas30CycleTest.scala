package amf.emit

import amf.core.remote._
import amf.io.{BuildCycleTests, FunSuiteCycleTests}

class Oas30CycleTest extends FunSuiteCycleTests {
  override val basePath: String = "amf-client/shared/src/test/resources/upanddown/oas3/"

  case class FixtureData(name: String, apiFrom: String, apiTo: String)

  val cycleOas3ToRaml10 = Seq(
    FixtureData("Basic servers", "basic-servers.json", "basic-servers.raml"),
    FixtureData("Complex servers", "complex-servers.json", "complex-servers.json.raml")
  )

  cycleOas3ToRaml10.foreach { f =>
    test(s"${f.name} - oas3 to raml10") {
      cycle(f.apiFrom, f.apiTo, OasJsonHint, Raml10)
    }
  }

  val cycleOas2ToOas3 = Seq(
    FixtureData("Basic servers", "basic-servers-2.json", "basic-servers-2.json.json")
  )

  cycleOas2ToOas3.foreach { f =>
    test(s"${f.name} - oas2 to oas3") {
      cycle(f.apiFrom, f.apiTo, OasJsonHint, Oas30)
    }
  }

  val cyclesOas3 = Seq(
    FixtureData("Basic servers", "basic-servers.json", "basic-servers.json"),
    FixtureData("Complex servers", "complex-servers.json", "complex-servers.json"),
    FixtureData("Basic content", "basic-content.json", "basic-content.json"),
    FixtureData("Basic encoding", "basic-encoding.json", "basic-encoding.json"),
    FixtureData("Basic request body", "basic-request-body.json", "basic-request-body.json"),
    FixtureData("Basic response headers", "basic-headers-response.json", "basic-headers-response.json"),
    FixtureData("Basic links", "basic-links.json", "basic-links.json")
  )

  cyclesOas3.foreach { f =>
    test(s"${f.name} - oas3 to oas3") {
      cycle(f.apiFrom, f.apiTo, OasJsonHint, Oas30)
    }
  }

  val cyclesRamlOas3 = Seq(
    FixtureData("Basic servers", "basic-servers.raml", "basic-servers.raml.json"),
    FixtureData("Complex servers", "complex-servers.raml", "complex-servers.json")
  )

  cyclesRamlOas3.foreach { f =>
    test(s"${f.name} - raml to oas3") {
      cycle(f.apiFrom, f.apiTo, RamlYamlHint, Oas30)
    }
  }

  val cyclesOas3Amf = Seq(
    FixtureData("Complex servers", "complex-servers.json", "complex-servers.jsonld")
  )

  cyclesOas3Amf.foreach { f =>
    test(s"${f.name} - oas3 to amf") {
      cycle(f.apiFrom, f.apiTo, OasJsonHint, Amf)
    }
  }
}

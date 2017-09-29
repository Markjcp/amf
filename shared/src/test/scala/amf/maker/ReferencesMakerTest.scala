package amf.maker

import amf.common.AmfObjectTestMatcher
import amf.compiler.AMFCompiler
import amf.document.Document
import amf.document.Fragment.{DataType, Fragment}
import amf.domain.WebApi
import amf.remote.{Hint, RamlYamlHint}
import amf.shape.NodeShape
import amf.unsafe.PlatformSecrets
import org.scalatest.{Assertion, AsyncFunSuite, Succeeded}

import scala.concurrent.{ExecutionContext, Future}

/**
  *
  */
class ReferencesMakerTest extends AsyncFunSuite with PlatformSecrets with AmfObjectTestMatcher {
  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Data type fragment test") {
    val rootDocument = "file://shared/src/test/resources/references/data-type-fragment.raml"
    assertFixture(usesDataType, rootDocument, RamlYamlHint)
  }

  private def assertFixture(rootExpected: Document, rootFile: String, hint: Hint): Future[Assertion] = {

    AMFCompiler(rootFile, platform, hint)
      .build()
      .map({
        case actual: Document => actual
      })
      .map({ actual =>
        AmfObjectMatcher(rootExpected).assert(actual)
        actual.references.zipWithIndex foreach {
          case (actualRef, index) =>
            AmfObjectMatcher(rootExpected.references(index)).assert(actualRef)
        }
        Succeeded
      })
  }

  private val person: NodeShape = {
    val shape = NodeShape().withClosed(false)
    shape
      .withProperty("name")
      .withMinCount(1)
      .withScalarSchema("name")
      .withDataType("http://www.w3.org/2001/XMLSchema#string")
    shape
  }

  private val dataTypeFragment: Fragment = {
    DataType()
      .withId("file:/shared/src/test/resources/references/fragments/person.raml")
      .withEncodes(person)
  }

  private val usesDataType: Document = {
    val shape = person.copy()
    shape.withName("person")
    Document()
      .withId("/Users/hernan.najles/mulesoft/amf/shared/src/test/resources/references/data-type-fragment.raml")
      .withEncodes(WebApi().withId("shared/src/test/resources/references/data-type-fragment.raml#/web-api"))
      .withDeclares(Seq(shape))
      .withReferences(Seq(dataTypeFragment))
  }

}

package amf.dialects
import amf.ProfileName
import amf.core.AMFCompiler
import amf.core.remote.Cache
import amf.core.services.RuntimeValidator
import amf.core.unsafe.PlatformSecrets
import amf.facades.Validation
import amf.io.FileAssertionTest
import amf.plugins.document.vocabularies.AMLPlugin
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance}
import org.scalatest.{Assertion, AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

class DialectDefinitionValidationTest extends AsyncFunSuite with Matchers with FileAssertionTest with PlatformSecrets {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  test("Test invalid property term uri for description") {
    validate("/schema-uri/dialect.yaml", Some("/schema-uri/report.json"), Some("/schema-uri/instance.yaml"))
  }

  private val path: String = "amf-client/shared/src/test/resources/vocabularies2/instances/invalids"

  protected def validate(dialect: String, goldenReport: Option[String], instance: Option[String]): Future[Assertion] = {
    amf.core.AMF.registerPlugin(AMLPlugin)
    amf.core.AMF.registerPlugin(AMFValidatorPlugin)
    val report = for {
      _ <- Validation(platform).map(_.withEnabledValidation(true))
      dialect <- {
        new AMFCompiler(
          "file://" + path + dialect,
          platform,
          None,
          Some("application/yaml"),
          Some(AMLPlugin.ID),
          cache = Cache()
        ).build()
      }
      i <- {
        instance match {
          case Some(i) =>
            new AMFCompiler(
              "file://" + path + i,
              platform,
              None,
              Some("application/yaml"),
              Some(AMLPlugin.ID),
              cache = Cache()
            ).build()
          case _ => Future.successful(DialectInstance())
        }
      }
      r <- {
        RuntimeValidator(
          dialect,
          ProfileName(dialect.asInstanceOf[Dialect].nameAndVersion())
        )
      }
    } yield r

    report.flatMap { re =>
      goldenReport match {
        case Some(r) =>
          writeTemporaryFile(path + r)(ValidationReportJSONLDEmitter.emitJSON(re))
            .flatMap(assertDifferences(_, path + r))
        case None => re.conforms should be(true)
      }
    }
  }
}

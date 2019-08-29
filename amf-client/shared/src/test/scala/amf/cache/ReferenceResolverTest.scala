package amf.cache

import amf.RamlProfile
import amf.client.convert.CoreClientConverters._
import amf.client.convert.NativeOps
import amf.client.environment.DefaultEnvironment
import amf.client.model.document.{Document, Module}
import amf.client.model.domain.{NodeShape, WebApi}
import amf.client.parse.RamlParser
import amf.client.reference.ReferenceResolver
import amf.client.resolve.Raml10Resolver
import amf.client.resource.ResourceNotFound
import amf.client.{AMF, reference}
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.internal.reference.CachedReference
import org.scalatest.{AsyncFunSuite, Matchers}

import scala.concurrent.{ExecutionContext, Future}

trait ReferenceResolverTest extends AsyncFunSuite with Matchers with NativeOps {

  override implicit val executionContext: ExecutionContext = ExecutionContext.Implicits.global

  case class CustomReferenceResolver(references: Seq[CachedReference]) extends ReferenceResolver {

    /** If the resource not exists, you should return a future failed with an [[ResourceNotFound]] exception. */
    override def fetch(url: String): ClientFuture[reference.CachedReference] =
      references.find(r => r.url == url) match {
        case Some(value) =>
          Future { value }.asClient
        case _ =>
          Future.failed[CachedReference](new ResourceNotFound("Reference not found")).asClient
      }
  }

  test("Without resolve - Simple API") {

    val path        = "file://amf-client/shared/src/test/resources/cache/api-library/"
    val libraryPath = "library.raml"
    val mainPath    = "api.raml"

    for {
      _       <- AMF.init().asFuture
      library <- new RamlParser().parseFileAsync(path + libraryPath).asFuture
      environment <- {
        val references = Seq(CachedReference(libraryPath, library, resolved = false))
        Future.successful(
          DefaultEnvironment().withResolver(CustomReferenceResolver(references).asInstanceOf[ClientReference])
        )
      }
      root   <- new RamlParser(environment).parseFileAsync(path + mainPath).asFuture
      report <- AMF.validate(root, RamlProfile, RamlProfile.messageStyle).asFuture
    } yield {
      assert(report.conforms)
      assert(root.references().asSeq.nonEmpty)
      assert(root.references().asSeq.head.isInstanceOf[Module])
    }
  }

  test("Without resolve - Multiple References Mixed") {

    val path        = "file://amf-client/shared/src/test/resources/cache/api-multiple-references/"
    val libraryPath = "library.raml"
    val type1Path   = "datatypeC.raml"
    val mainPath    = "api.raml"

    for {
      _         <- AMF.init().asFuture
      library   <- new RamlParser().parseFileAsync(path + libraryPath).asFuture
      datatype1 <- new RamlParser().parseFileAsync(path + type1Path).asFuture
      environment <- {
        val references = Seq(CachedReference(libraryPath, library, resolved = false),
                             CachedReference(type1Path, datatype1, resolved = false))
        Future.successful(
          DefaultEnvironment().withResolver(CustomReferenceResolver(references).asInstanceOf[ClientReference])
        )
      }
      root   <- new RamlParser(environment).parseFileAsync(path + mainPath).asFuture
      report <- AMF.validate(root, RamlProfile, RamlProfile.messageStyle).asFuture
    } yield {
      assert(report.conforms)
      assert(root.references().asSeq.nonEmpty)
      assert(root.references().asSeq.size == 3)
    }
  }

  test("Without resolve - rt with reference of root type declaration") {

    val path     = "file://amf-client/shared/src/test/resources/cache/api-rt/"
    val rtPath   = "rt.raml"
    val mainPath = "api.raml"

    for {
      _  <- AMF.init().asFuture
      rt <- new RamlParser().parseFileAsync(path + rtPath).asFuture
      environment <- {
        val references = Seq(CachedReference(rtPath, rt, resolved = false))
        Future.successful(
          DefaultEnvironment().withResolver(CustomReferenceResolver(references).asInstanceOf[ClientReference])
        )
      }
      root   <- new RamlParser(environment).parseFileAsync(path + mainPath).asFuture
      report <- AMF.validate(root, RamlProfile, RamlProfile.messageStyle).asFuture
    } yield {
      assert(report.conforms)
      assert(root.references().asSeq.nonEmpty)
    }
  }

  test("Without resolve - trait with reference of root type declaration") {

    val path      = "file://amf-client/shared/src/test/resources/cache/api-trait/"
    val traitPath = "trait.raml"
    val mainPath  = "api.raml"

    for {
      _  <- AMF.init().asFuture
      tr <- new RamlParser().parseFileAsync(path + traitPath).asFuture
      environment <- {
        val references = Seq(CachedReference(traitPath, tr, resolved = false))
        Future.successful(
          DefaultEnvironment().withResolver(CustomReferenceResolver(references).asInstanceOf[ClientReference])
        )
      }
      root   <- new RamlParser(environment).parseFileAsync(path + mainPath).asFuture
      report <- AMF.validate(root, RamlProfile, RamlProfile.messageStyle).asFuture
    } yield {
      assert(report.conforms)
      assert(root.references().asSeq.nonEmpty)
    }
  }

  test("Resolved - Library fragment with complex types") {

    val path     = "file://amf-client/shared/src/test/resources/cache/api-complex-lib-1/"
    val libPath  = "library.raml"
    val mainPath = "api.raml"

    for {
      _               <- AMF.init().asFuture
      library         <- new RamlParser().parseFileAsync(path + libPath).asFuture
      libraryResolved <- Future(new Raml10Resolver().resolve(library, ResolutionPipeline.EDITING_PIPELINE))
      environment <- {
        val references = Seq(CachedReference(libPath, libraryResolved, resolved = false))
        Future.successful(
          DefaultEnvironment().withResolver(CustomReferenceResolver(references).asInstanceOf[ClientReference])
        )
      }
      root   <- new RamlParser(environment).parseFileAsync(path + mainPath).asFuture
      report <- AMF.validate(root, RamlProfile, RamlProfile.messageStyle).asFuture
    } yield {
      assert(report.conforms)
      assert(root.references().asSeq.nonEmpty)
      assert(
        root
          .asInstanceOf[Document]
          .encodes
          .asInstanceOf[WebApi]
          .endPoints
          .asSeq
          .head
          .operations
          .asSeq(1)
          .request
          .payloads
          .asSeq
          .head
          .schema
          .asInstanceOf[NodeShape]
          .properties
          .asSeq
          .size == 6)
    }
  }

  test("Resolved - Library fragment with complex rt") {

    val path     = "file://amf-client/shared/src/test/resources/cache/api-complex-lib-2/"
    val libPath  = "library.raml"
    val mainPath = "api.raml"

    for {
      _               <- AMF.init().asFuture
      library         <- new RamlParser().parseFileAsync(path + libPath).asFuture
      libraryResolved <- Future(new Raml10Resolver().resolve(library, ResolutionPipeline.EDITING_PIPELINE))
      environment <- {
        val references = Seq(CachedReference(libPath, libraryResolved, resolved = false))
        Future.successful(
          DefaultEnvironment().withResolver(CustomReferenceResolver(references).asInstanceOf[ClientReference])
        )
      }
      root   <- new RamlParser(environment).parseFileAsync(path + mainPath).asFuture
      report <- AMF.validate(root, RamlProfile, RamlProfile.messageStyle).asFuture
    } yield {
      assert(report.conforms)
      assert(root.references().asSeq.nonEmpty)
      assert(
        root
          .asInstanceOf[Document]
          .encodes
          .asInstanceOf[WebApi]
          .endPoints
          .asSeq
          .head
          .operations
          .asSeq(1)
          .request
          .payloads
          .asSeq
          .head
          .schema
          .asInstanceOf[NodeShape]
          .properties
          .asSeq
          .size == 2)
    }
  }

}

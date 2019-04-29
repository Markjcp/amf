package amf.plugins.document.webapi.parser.spec.domain

import amf.core.annotations.TrackedElement
import amf.core.model.domain.AmfArray
import amf.core.parser.{Annotations, ScalarNode, SearchScope, _}
import amf.core.utils.Strings
import amf.plugins.document.webapi.annotations.DefaultPayload
import amf.plugins.document.webapi.contexts.OasWebApiContext
import amf.plugins.document.webapi.parser.spec.OasDefinitions
import amf.plugins.document.webapi.parser.spec.common.{AnnotationParser, SpecParserOps}
import amf.plugins.document.webapi.parser.spec.declaration.OasTypeParser
import amf.plugins.domain.shapes.models.ExampleTracking.tracking
import amf.plugins.domain.shapes.models.Examples
import amf.plugins.domain.webapi.metamodel.{PayloadModel, RequestModel, ResponseModel}
import amf.plugins.domain.webapi.models.{Parameter, Payload, Response}
import org.yaml.model.{YMap, YMapEntry}

import scala.collection.mutable

case class OasResponseParser(entry: YMapEntry, adopted: Response => Unit)(implicit ctx: OasWebApiContext)
    extends SpecParserOps {
  def parse(): Response = {

    val node = ScalarNode(entry.key).text()

    val response: Response = ctx.link(entry.value) match {
      case Left(url) =>
        val name = OasDefinitions.stripResponsesDefinitionsPrefix(url)
        val response: Response = ctx.declarations
          .findResponseOrError(entry.value)(name, SearchScope.Named)
          .link(OasDefinitions.stripResponsesDefinitionsPrefix(url))
        adopted(response.set(ResponseModel.Name, node))
        response.annotations ++= Annotations(entry)
        response
      case Right(value) =>
        val map = value.as[YMap]
        val res = Response(entry).set(ResponseModel.Name, node)
        adopted(res)

        map.key("description", ResponseModel.Description in res)

        map.key(
          "headers",
          entry => {
            val parameters: Seq[Parameter] =
              OasHeaderParametersParser(entry.value.as[YMap], res.withHeader).parse()
            res.set(RequestModel.Headers, AmfArray(parameters, Annotations(entry.value)), Annotations(entry))
          }
        )

        val payloads = mutable.ListBuffer[Payload]()

        defaultPayload(map, res.id).foreach(payloads += _)

        map.key(
          "responsePayloads".asOasExtension,
          entry =>
            entry.value
              .as[Seq[YMap]]
              .map(value => payloads += OasPayloadParser(value, res.withPayload).parse())
        )

        if (payloads.nonEmpty)
          res.set(ResponseModel.Payloads, AmfArray(payloads))

        val examples = OasResponseExamplesParser("examples", map).parse()
        if (examples.nonEmpty) {
          val ex = Examples()
          res.set(ResponseModel.Examples, ex)
          ex.withExamples(examples)
        }

        AnnotationParser(res, map).parse()

        ctx.closedShape(res.id, map, "response")

        res
    }

//    if (!response.annotations.contains(classOf[DeclaredElement])) {
//      if (response.name.is("default")) {
//        response.set(ResponseModel.StatusCode, "200")
//      } else {
//        response.set(ResponseModel.StatusCode, node)
//      }
//    }

    response
  }

  private def defaultPayload(entries: YMap, parentId: String): Option[Payload] = {
    val payload = Payload().add(DefaultPayload())

    entries.key("mediaType".asOasExtension,
                entry => payload.set(PayloadModel.MediaType, ScalarNode(entry.value).string(), Annotations(entry)))
    // TODO add parent id to payload?
    payload.adopted(parentId)

    entries.key(
      "schema",
      entry =>
        OasTypeParser(entry, shape => shape.withName("default").adopted(payload.id))
          .parse()
          .map { s =>
            payload.set(PayloadModel.Schema, tracking(s, payload.id), Annotations(entry))
        }
    )

    if (payload.fields.nonEmpty) Some(payload) else None
  }
}

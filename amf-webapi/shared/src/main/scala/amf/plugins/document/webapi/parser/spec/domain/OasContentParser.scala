package amf.plugins.document.webapi.parser.spec.domain

import amf.core.annotations.TrackedElement
import amf.core.model.domain.AmfArray
import amf.core.parser.{Annotations, _}
import amf.plugins.document.webapi.contexts.OasWebApiContext
import amf.plugins.document.webapi.parser.spec.common.{AnnotationParser, SpecParserOps}
import amf.plugins.document.webapi.parser.spec.declaration.OasTypeParser
import amf.plugins.domain.shapes.models.ExampleTracking.tracking
import amf.plugins.domain.webapi.metamodel.PayloadModel
import amf.plugins.domain.webapi.models.Payload
import org.yaml.model.{YMap, YNode}

case class OasContentParser(node: YNode, mediaType: String, producer: Option[String] => Payload)(implicit ctx: OasWebApiContext)
  extends SpecParserOps {

  def parse(): Payload = {
    val map = node.as[YMap]
    val payload = producer(Some(mediaType)).add(Annotations.valueNode(map))


    // schema
    map.key(
      "schema",
      entry => {
        OasTypeParser(entry, shape => shape.withName("schema").adopted(payload.id))
          .parse()
          .map(s => payload.set(PayloadModel.Schema, tracking(s, payload.id), Annotations(entry)))
      }
    )


    // example -> ignore TODO

    // examples
    map.key(
      "examples",
      entry => {
        val examples = Oas3ResponseExamplesParser(entry).parse()
        if (examples.nonEmpty) {
          examples.foreach { ex =>
            ex.withMediaType(mediaType)
            ex.annotations += TrackedElement(payload.id)
          }
        }
        payload.set(PayloadModel.Examples, AmfArray(examples, Annotations(entry.value)), Annotations(entry))
      }
    )

    // encoding
    map.key(
      "encoding",
      entry => {
        val encodings = OasEncodingParser(entry.value.as[YMap], payload.withEncoding).parse()
        payload.setArray(PayloadModel.Encoding, encodings, Annotations(entry))
      }
    )


    AnnotationParser(payload, map).parse()

    ctx.closedShape(payload.id, map, "content")

    payload
  }

}

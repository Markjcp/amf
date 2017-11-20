package amf.spec.domain

import amf.common.Lazy
import amf.domain.{Annotations, Parameter, Payload, Request}
import amf.metadata.domain.RequestModel
import amf.model.AmfArray
import amf.parser.YMapOps
import amf.plugins.domain.webapi.contexts.WebApiContext
import amf.spec.ParserContext
import amf.spec.common.AnnotationParser
import amf.spec.declaration.RamlTypeParser
import org.yaml.model.YMap

import scala.collection.mutable

/**
  *
  */
case class RamlRequestParser(map: YMap, producer: () => Request)(implicit ctx: WebApiContext) {

  def parse(): Option[Request] = {
    val request = new Lazy[Request](producer)
    map.key(
      "queryParameters",
      entry => {

        val parameters: Seq[Parameter] =
          RamlParametersParser(entry.value.as[YMap], request.getOrCreate.withQueryParameter)
            .parse()
            .map(_.withBinding("query"))
        request.getOrCreate.set(RequestModel.QueryParameters,
                                AmfArray(parameters, Annotations(entry.value)),
                                Annotations(entry))
      }
    )

    map.key(
      "headers",
      entry => {
        val parameters: Seq[Parameter] =
          RamlParametersParser(entry.value.as[YMap], request.getOrCreate.withHeader)
            .parse()
            .map(_.withBinding("header"))
        request.getOrCreate.set(RequestModel.Headers,
                                AmfArray(parameters, Annotations(entry.value)),
                                Annotations(entry))
      }
    )

    map.key(
      "queryString",
      queryEntry => {
        RamlTypeParser(queryEntry, (shape) => shape.adopted(request.getOrCreate.id))
          .parse()
          .map(q => request.getOrCreate.withQueryString(q))
      }
    )

    map.key(
      "body",
      entry => {
        val payloads = mutable.ListBuffer[Payload]()

        val bodyMap = entry.value.as[YMap]
        RamlTypeParser(entry, shape => shape.withName("default").adopted(request.getOrCreate.id))
          .parse()
          .foreach(payloads += request.getOrCreate.withPayload(None).withSchema(_)) // todo

        entry.value
          .as[YMap]
          .regex(
            ".*/.*",
            entries => {
              entries.foreach(entry => {
                payloads += RamlPayloadParser(entry, producer = request.getOrCreate.withPayload)
                  .parse()
              })
            }
          )
        if (payloads.nonEmpty)
          request.getOrCreate
            .set(RequestModel.Payloads, AmfArray(payloads, Annotations(entry.value)), Annotations(entry))
      }
    )

    AnnotationParser(() => request.getOrCreate, map).parse()

    request.option
  }
}

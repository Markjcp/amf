package amf.plugins.domain.webapi.models

import amf.core.metamodel.{Field, Obj}
import amf.core.model.StrField
import amf.core.model.domain._
import amf.core.parser.{Annotations, Fields}
import amf.plugins.domain.shapes.models.{Example, Examples}
import amf.plugins.domain.webapi.metamodel.ResponseModel
import amf.plugins.domain.webapi.metamodel.ResponseModel._
import amf.core.utils.Strings
import org.yaml.model.YMapEntry

/**
  * Response internal model.
  */
class Response(override val fields: Fields, override val annotations: Annotations)
    extends DomainElement
    with Linkable
    with NamedDomainElement {

  def description: StrField     = fields.field(Description)
  def statusCode: StrField      = fields.field(StatusCode)
  def headers: Seq[Parameter]   = fields.field(Headers)
  def payloads: Seq[Payload]    = fields.field(Payloads)
  def links: Seq[TemplatedLink] = fields.field(Links)
  def examples: Examples        = fields.field(ResponseModel.Examples)

  def withDescription(description: String): this.type = set(Description, description)
  def withStatusCode(statusCode: String): this.type   = set(StatusCode, statusCode)
  def withHeaders(headers: Seq[Parameter]): this.type = setArray(Headers, headers)
  def withPayloads(payloads: Seq[Payload]): this.type = setArray(Payloads, payloads)
  def withLinks(links: Seq[TemplatedLink]): this.type = setArray(Links, links)
  def withExamples(examples: Examples): this.type     = set(ResponseModel.Examples, examples)
  def withExamples(examples: Seq[Example]): this.type = {
    val ex = Examples()
    set(ResponseModel.Examples, ex)
    ex.withExamples(examples)
    this
  }

  def withHeader(name: String): Parameter = {
    val result = Parameter().withName(name)
    add(Headers, result)
    result
  }

  def withPayload(mediaType: Option[String] = None): Payload = {
    val result = Payload()
    mediaType.map(result.withMediaType)
    add(Payloads, result)
    result
  }

  def withExample(mediaType: String): Example = {
    val e = examples match {
      case e: Examples => e
      case _ =>
        val newExamples = Examples()
        withExamples(newExamples)
        newExamples
    }

    e.withExampleWithMediaType(mediaType)
  }

  def withExample(newExample: Example): Examples = {
    examples match {
      case e: Examples => e ++ Seq(newExample)
      case _ =>
        val newExamples = Examples()
        withExamples(newExamples)
        newExamples.withExamples(Seq(newExample))
    }
    examples
  }

  def exampleValues: Seq[Example] = examples match {
    case e: Examples => e.examples
    case _           => Nil
  }

  def cloneResponse(parent: String): Response = {
    val cloned = Response(annotations).withName(name.value()).adopted(parent)

    this.fields.foreach {
      case (f, v) =>
        val clonedValue = v.value match {
          case a: AmfArray =>
            AmfArray(a.values.map {
              case p: Parameter => p.cloneParameter(cloned.id)
              case p: Payload   => p.clonePayload(cloned.id)
              case o            => o
            }, a.annotations)
          case o => o
        }

        cloned.set(f, clonedValue, v.annotations)
    }

    cloned.asInstanceOf[this.type]
  }

  override def meta: Obj = ResponseModel

  override def linkCopy(): Linkable = Response().withId(id)

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String = "/" + name.option().getOrElse("default-response").urlComponentEncoded

  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement = Response.apply
  override protected def nameField: Field                                                       = Name
}

object Response {
  def apply(): Response = apply(Annotations())

  def apply(entry: YMapEntry): Response = apply(Annotations(entry))

  def apply(annotations: Annotations): Response = new Response(Fields(), annotations)

  def apply(fields: Fields, annotations: Annotations): Response = new Response(fields, annotations)
}

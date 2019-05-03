package amf.plugins.domain.webapi.models

import amf.core.metamodel.{Field, Obj}
import amf.core.model.StrField
import amf.core.model.domain.{DomainElement, Linkable, NamedDomainElement, Shape}
import amf.core.parser.{Annotations, Fields}
import amf.core.utils.Strings
import amf.plugins.domain.shapes.models.{ArrayShape, Example, NodeShape, ScalarShape}
import amf.plugins.domain.webapi.metamodel.PayloadModel
import amf.plugins.domain.webapi.metamodel.PayloadModel.{Encoding => EncodingModel, _}
import org.yaml.model.YPart

/**
  * Payload internal model.
  */
case class Payload(fields: Fields, annotations: Annotations)
    extends DomainElement
    with Linkable
    with NamedDomainElement {

  def mediaType: StrField     = fields.field(MediaType)
  def schema: Shape           = fields.field(Schema)
  def examples: Seq[Example]  = fields.field(Examples)
  def encoding: Seq[Encoding] = fields.field(EncodingModel)

  def withMediaType(mediaType: String): this.type      = set(MediaType, mediaType)
  def withSchema(schema: Shape): this.type             = set(Schema, schema)
  def withExamples(examples: Seq[Example]): this.type  = setArray(Examples, examples)
  def withEncoding(encoding: Seq[Encoding]): this.type = setArray(EncodingModel, encoding)

  def withObjectSchema(name: String): NodeShape = {
    val node = NodeShape().withName(name)
    set(PayloadModel.Schema, node)
    node
  }

  def withScalarSchema(name: String): ScalarShape = {
    val scalar = ScalarShape().withName(name)
    set(PayloadModel.Schema, scalar)
    scalar
  }

  def withArraySchema(name: String): ArrayShape = {
    val array = ArrayShape().withName(name)
    set(PayloadModel.Schema, array)
    array
  }

  def withExample(name: Option[String] = None): Example = {
    val example = Example()
    name.foreach { example.withName(_) }
    add(Examples, example)
    example
  }

  def withEncoding(name: String): Encoding = {
    val result = Encoding().withPropertyName(name)
    add(EncodingModel, result)
    result
  }

  override def linkCopy(): Payload = Payload().withId(id)

  def clonePayload(parent: String): Payload = {
    val cloned = Payload(annotations).withMediaType(mediaType.value()).adopted(parent)

    this.fields.foreach {
      case (f, v) =>
        val clonedValue = v.value match {
          case s: Shape => s.cloneShape(None)
          case o        => o
        }

        cloned.set(f, clonedValue, v.annotations)
    }

    cloned.asInstanceOf[this.type]
  }

  override def meta: Obj = PayloadModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String =
    "/" + mediaType
      .option()
      .getOrElse(name.option().getOrElse("default"))
      .urlComponentEncoded // todo: / char of media type should be encoded?
  /** apply method for create a new instance with fields and annotations. Aux method for copy */
  override protected def classConstructor: (Fields, Annotations) => Linkable with DomainElement = Payload.apply
  override protected def nameField: Field                                                       = Name
}

object Payload {
  def apply(): Payload = apply(Annotations())

  def apply(ast: YPart): Payload = apply(Annotations(ast))

  def apply(annotations: Annotations): Payload = new Payload(Fields(), annotations)
}

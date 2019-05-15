package amf.plugins.domain.webapi.models

import amf.core.metamodel.Field
import amf.core.model.StrField
import amf.core.model.domain.NamedDomainElement
import amf.core.parser.{Annotations, Fields}
import amf.core.utils.Strings
import amf.plugins.domain.webapi.metamodel.CallbackModel
import amf.plugins.domain.webapi.metamodel.CallbackModel._
import org.yaml.model.YMap

/**
  * Callback internal model
  */
case class Callback(fields: Fields, annotations: Annotations) extends NamedDomainElement {

  def expression: StrField = fields.field(Expression)
  def endpoint: EndPoint   = fields.field(Endpoint)

  def withExpression(expression: String): this.type = set(Expression, expression)
  def withEndpoint(endpoint: EndPoint): this.type   = set(Endpoint, endpoint)

  def withEndpoint(): EndPoint = {
    val result = EndPoint()
    set(Endpoint, result)
    result
  }

  override def meta = CallbackModel

  /** Value , path + field value that is used to compose the id when the object its adopted */
  override def componentId: String        = "/" + name.option().getOrElse("default-callback").urlComponentEncoded
  override protected def nameField: Field = Name
}

object Callback {

  def apply(): Callback = apply(Annotations())

  def apply(ast: YMap): Callback = apply(Annotations(ast))

  def apply(annotations: Annotations): Callback = new Callback(Fields(), annotations)
}

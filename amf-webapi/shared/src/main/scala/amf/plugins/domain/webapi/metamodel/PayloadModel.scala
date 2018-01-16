package amf.plugins.domain.webapi.metamodel

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Bool, Str}
import amf.core.metamodel.domain.{DomainElementModel, ShapeModel}
import amf.core.metamodel.domain.templates.{KeyField, OptionalField}
import amf.plugins.domain.webapi.models.Payload
import amf.core.vocabulary.Namespace.Http
import amf.core.vocabulary.{Namespace, ValueType}

/**
  * Payload metamodel.
  */
object PayloadModel extends DomainElementModel with KeyField with OptionalField {

  val MediaType = Field(Str, Http + "mediaType")

  val Schema = Field(ShapeModel, Http + "schema")

  override val key: Field = MediaType

  override val `type`: List[ValueType] = Http + "Payload" :: DomainElementModel.`type`

  override def fields: List[Field] = MediaType :: Schema :: DomainElementModel.fields

  override def modelInstance = Payload()
}

package amf.plugins.domain.webapi.metamodel

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Array, Str}
import amf.core.metamodel.domain.common.NameFieldSchema
import amf.core.metamodel.domain.templates.{KeyField, OptionalField}
import amf.core.metamodel.domain._
import amf.core.vocabulary.Namespace.Http
import amf.core.vocabulary.ValueType
import amf.plugins.domain.shapes.metamodel.common.ExampleField
import amf.plugins.domain.webapi.models.Payload

/**
  * Payload metamodel.
  */
object PayloadModel
    extends DomainElementModel
    with KeyField
    with OptionalField
    with NameFieldSchema
    with LinkableElementModel
    with ExampleField {

  val MediaType = Field(Str,
                        Http + "mediaType",
                        ModelDoc(ModelVocabularies.Http, "media type", "Media types supported in the payload"))

  val Schema =
    Field(ShapeModel, Http + "schema", ModelDoc(ModelVocabularies.Http, "schema", "Schema associated to this payload"))

  val Encoding = Field(Array(EncodingModel), Http + "encoding", ModelDoc(ModelVocabularies.Http, "encoding", ""))

  override val key: Field = MediaType

  override val `type`: List[ValueType] = Http + "Payload" :: DomainElementModel.`type`

  override def fields: List[Field] =
    Name :: MediaType :: Schema :: Examples :: Encoding :: (DomainElementModel.fields ++ LinkableElementModel.fields)

  override def modelInstance = Payload()

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.Http,
    "Payload",
    "Encoded payload using certain media-type"
  )
}

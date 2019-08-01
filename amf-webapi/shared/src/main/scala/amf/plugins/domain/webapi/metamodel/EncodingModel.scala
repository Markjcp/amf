package amf.plugins.domain.webapi.metamodel

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Array, Bool, Str}
import amf.core.metamodel.domain.{DomainElementModel, ModelDoc, ModelVocabularies}
import amf.core.metamodel.domain.templates.KeyField
import amf.core.vocabulary.Namespace.ApiContract
import amf.core.vocabulary.ValueType
import amf.plugins.domain.webapi.models.Encoding

/**
  * Encoding metamodel.
  */
object EncodingModel extends DomainElementModel with KeyField {

  val PropertyName = Field(Str, ApiContract + "propertyName", ModelDoc(ModelVocabularies.ApiContract, "property name", ""))

  val ContentType = Field(Str, ApiContract + "contentType", ModelDoc(ModelVocabularies.ApiContract, "content type", ""))

  val Headers = Field(Array(ParameterModel), ApiContract + "header", ModelDoc(ModelVocabularies.ApiContract, "header", ""))

  val Style = Field(Str, ApiContract + "style", ModelDoc(ModelVocabularies.ApiContract, "style", ""))

  val Explode = Field(Bool, ApiContract + "explode", ModelDoc(ModelVocabularies.ApiContract, "explode", ""))

  val AllowReserved = Field(Bool, ApiContract + "allowReserved", ModelDoc(ModelVocabularies.ApiContract, "allow reserved", ""))

  override val `type`: List[ValueType] = ApiContract + "Encoding" :: DomainElementModel.`type`

  override def fields: List[Field] =
    PropertyName :: ContentType :: Headers :: Style :: Explode :: AllowReserved :: DomainElementModel.fields

  override def modelInstance = Encoding()

  override val key: Field = PropertyName

  // TODO: doc, describe this model
  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.ApiContract,
    "Encoding",
    ""
  )
}

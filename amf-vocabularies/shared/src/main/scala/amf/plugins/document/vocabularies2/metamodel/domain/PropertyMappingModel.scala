package amf.plugins.document.vocabularies2.metamodel.domain

import amf.core.metamodel.Field
import amf.core.metamodel.Type.{Bool, Int, Iri, SortedArray, Str, Any}
import amf.core.metamodel.domain.DomainElementModel
import amf.core.model.domain.AmfObject
import amf.core.vocabulary.{Namespace, ValueType}
import amf.plugins.document.vocabularies2.model.domain.PropertyMapping


object PropertyMappingModel extends DomainElementModel {

  val Name                 = Field(Str, Namespace.Schema + "name")
  val NodePropertyMapping   = Field(Iri, Namespace.Shacl + "path")
  val LiteralRange          = Field(Iri, Namespace.Shacl + "datatype")
  val ObjectRange           = Field(SortedArray(Iri), Namespace.Shacl + "node")
  val MapKeyProperty        = Field(Iri, Namespace.Meta + "mapProperty")
  val MapValueProperty      = Field(Iri, Namespace.Meta + "mapValueProperty")
  val Sorted                = Field(Bool, Namespace.Meta + "sorted")
  val MinCount              = Field(Int, Namespace.Shacl + "minCount")
  val Pattern               = Field(Str, Namespace.Shacl + "pattern")
  val Minimum               = Field(Int, Namespace.Shacl + "minInclusive")
  val Maximum               = Field(Int, Namespace.Shacl + "maxInclusive")
  val AllowMultiple         = Field(Bool, Namespace.Meta + "allowMultiple")
  val Enum                  = Field(SortedArray(Any), Namespace.Shacl + "in")
  val TypeDiscriminator     = Field(Str, Namespace.Meta + "typeDiscriminatorMap")
  val TypeDiscriminatorName = Field(Str, Namespace.Meta + "typeDiscriminatorName")

  override def fields: List[Field] = NodePropertyMapping :: Name :: LiteralRange :: ObjectRange :: MapKeyProperty ::
    MapValueProperty :: MinCount :: Pattern :: Minimum :: Maximum :: AllowMultiple :: Sorted :: Enum :: TypeDiscriminator ::
    TypeDiscriminatorName :: DomainElementModel.fields

  override def modelInstance: AmfObject = PropertyMapping()

  override val `type`: List[ValueType] = Namespace.Meta + "NodePropertyMapping" :: Namespace.Shacl + "PropertyShape" :: DomainElementModel.`type`

}

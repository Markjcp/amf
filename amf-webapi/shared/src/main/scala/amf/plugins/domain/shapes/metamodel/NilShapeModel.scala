package amf.plugins.domain.shapes.metamodel

import amf.core.metamodel.domain.{ModelDoc, ModelVocabularies, ShapeModel}
import amf.core.vocabulary.Namespace._
import amf.core.vocabulary.ValueType
import amf.plugins.domain.shapes.models.NilShape

object NilShapeModel extends AnyShapeModel {

  override val `type`: List[ValueType] =
    List(Shapes + "NilShape") ++ ShapeModel.`type`

  override def modelInstance = NilShape()

  override val doc: ModelDoc = ModelDoc(
    ModelVocabularies.Shapes,
    "Nil Shape",
    "Data shape representing the null/nil value in the input schema"
  )
}

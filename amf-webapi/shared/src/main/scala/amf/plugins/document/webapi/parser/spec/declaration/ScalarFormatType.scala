package amf.plugins.document.webapi.parser.spec.declaration

import amf.core.model.domain.Shape
import amf.core.parser._
import amf.plugins.document.webapi.contexts.WebApiContext
import amf.plugins.document.webapi.parser.spec.common.SpecParserOps
import amf.plugins.domain.shapes.metamodel.ScalarShapeModel
import amf.plugins.domain.shapes.models.TypeDef
import amf.plugins.domain.shapes.models.TypeDef.{DoubleType, FloatType, IntType, LongType}
import amf.plugins.domain.shapes.parser.XsdTypeDefMapping
import org.yaml.model.{YMap, YScalar}

object FormatValidator {
  def isValid(format: String, typeDef: TypeDef): Boolean = {
    if (typeDef.isNumber)
      typeDef match {
        case TypeDef.IntType if !List("int", "int8", "int16", "int32","int64", "long" ).contains(format) => false
        case FloatType if format.equals("double")                                       => false // should no be possible
        case LongType if List("double", "float").contains(format)                       => false // should not be possible either
        case other if !List("int", "int8", "int16", "int32", "int64","float", "double").contains(format) =>
          false
        case _ => true
      } else !List("int", "int8", "int16", "int32", "int64", "long", "float", "double").contains(format)
  }
}


case class ScalarFormatType(shape:Shape, typeDef: TypeDef)(implicit ctx:WebApiContext) extends SpecParserOps {
  def parse(map: YMap): TypeDef = {
    map.key("format").map { n =>
      val format = n.value.as[YScalar].text

      if (!FormatValidator.isValid(format, typeDef))
        ctx.warning(shape.id, s"Format $format is not valid for type ${XsdTypeDefMapping.xsd(typeDef)}", n)

      (ScalarShapeModel.Format in shape).allowingAnnotations(n)
      fromFormat(format)
    }.getOrElse(typeDef)
  }

  private def fromFormat(format:String) = {
    if(typeDef.isNumber)formatType(format)
    else typeDef
  }

  private def formatType(format:String) = {
    format match {
      case "int" | "int8" | "int16" | "int32" => IntType
      case "int64" | "long" => LongType
      case "double" => DoubleType
      case "float" => FloatType


    }
  }

}
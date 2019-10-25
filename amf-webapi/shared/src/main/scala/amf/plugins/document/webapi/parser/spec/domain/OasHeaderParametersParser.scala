package amf.plugins.document.webapi.parser.spec.domain

import amf.core.parser.{Annotations, ScalarNode, _}
import amf.plugins.document.webapi.contexts.OasWebApiContext
import amf.plugins.document.webapi.parser.spec.OasDefinitions
import amf.plugins.document.webapi.parser.spec.WebApiDeclarations.ErrorParameter
import amf.plugins.document.webapi.parser.spec.common.{AnnotationParser, SpecParserOps}
import amf.plugins.document.webapi.parser.spec.declaration.OasTypeParser
import amf.plugins.document.webapi.parser.spec.oas.Oas3Syntax
import amf.plugins.domain.shapes.models.ExampleTracking.tracking
import amf.plugins.domain.webapi.metamodel.ParameterModel
import amf.plugins.domain.webapi.models.Parameter
import amf.plugins.features.validation.CoreValidations
import org.yaml.model.YMap

case class OasHeaderParametersParser(map: YMap, adopt: Parameter => Unit)(implicit ctx: OasWebApiContext) {
  def parse(): Seq[Parameter] = {
    map.entries
      .map(entry =>
        OasHeaderParameterParser(entry.value.as[YMap], { header =>
          header.add(Annotations(entry))
          header.set(ParameterModel.Name, ScalarNode(entry.key).string())
          adopt(header)
        }).parse())
  }
}

case class OasHeaderParameterParser(map: YMap, adopt: Parameter => Unit)(implicit ctx: OasWebApiContext)
    extends SpecParserOps {
  def parse(): Parameter = {

    def commonHeader: Parameter = {
      val parameter = Parameter()
      adopt(parameter)
      map.key("description", ParameterModel.Description in parameter)
      AnnotationParser(parameter, map).parse()
      parameter
    }

    val header: Parameter = if (ctx.syntax == Oas3Syntax) {
      ctx.link(map) match {
        case Left(fullRef) =>
          val label = OasDefinitions.stripOas3ComponentsPrefix(fullRef, "headers")
          ctx.declarations
            .findHeader(label, SearchScope.Named)
            .map(header => {
              val linkHeader: Parameter = header.link(label)
              adopt(linkHeader)
              linkHeader
            })
            .getOrElse {
              ctx.obtainRemoteYNode(fullRef) match {
                case Some(requestNode) =>
                  OasHeaderParameterParser(requestNode.as[YMap], adopt).parse()
                case None =>
                  ctx.violation(CoreValidations.UnresolvedReference, "", s"Cannot find header reference $fullRef", map)
                  ErrorParameter(label, map)
              }
            }
        case Right(_) =>
          val header = commonHeader
          parseOas3Header(header, map)
          header
      }
    } else {
      val header = commonHeader
      parseOas2Header(header, map)
      header
    }
    header.withBinding("header") // we need to add the binding in order to conform all parameters validations
    header
  }

  protected def parseOas2Header(parameter: Parameter, map: YMap): Unit = {
    val name = Option(parameter.name).map(_.value())
    parameter.set(ParameterModel.Required, !name.exists(_.endsWith("?")))

    map.key("x-amf-required", (ParameterModel.Required in parameter).explicit)

    map.key(
      "type",
      _ => {
        OasTypeParser(map, name.getOrElse("default"), (shape) => shape.withName("schema").adopted(parameter.id))
          .parse()
          .map(s => parameter.set(ParameterModel.Schema, tracking(s, parameter.id), Annotations(map)))
      }
    )
  }

  protected def parseOas3Header(parameter: Parameter, map: YMap): Unit = {
    map.key("required", (ParameterModel.Required in parameter).explicit)
    map.key("deprecated", (ParameterModel.Deprecated in parameter).explicit)
    map.key("allowEmptyValue", (ParameterModel.AllowEmptyValue in parameter).explicit)

    map.key(
      "schema",
      entry => {
        OasTypeParser(entry, (shape) => shape.withName("schema").adopted(parameter.id))
          .parse()
          .map(s => parameter.set(ParameterModel.Schema, tracking(s, parameter.id), Annotations(entry)))
      }
    )

    ctx.closedShape(parameter.id, map, "header")
  }
}

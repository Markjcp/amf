package amf.plugins.document.webapi.parser.spec.domain

import amf.core.annotations.{BasePathLexicalInformation, HostLexicalInformation, SynthesizedField}
import amf.core.model.DataType
import amf.core.model.domain.{AmfArray, AmfScalar}
import amf.core.parser.{Annotations, _}
import amf.core.utils.{Strings, TemplateUri}
import amf.plugins.document.webapi.contexts.{OasWebApiContext, RamlWebApiContext}
import amf.plugins.document.webapi.parser.spec.common.{AnnotationParser, RamlScalarNode, SpecParserOps}
import amf.plugins.document.webapi.parser.spec.{toOas, toRaml}
import amf.plugins.domain.webapi.metamodel.{ServerModel, WebApiModel}
import amf.plugins.domain.webapi.models.{Parameter, Server, WebApi}
import amf.validations.ParserSideValidations._
import org.yaml.model.{YMap, YType}

case class RamlServersParser(map: YMap, api: WebApi)(implicit val ctx: RamlWebApiContext) extends SpecParserOps {
  def parse(): Unit = {
    map.key("baseUri") match {
      case Some(entry) =>
        val node   = RamlScalarNode(entry.value)
        val value  = node.text().toString
        val server = api.withServer(value)

        (ServerModel.Url in server).allowingAnnotations(entry)

        checkBalancedParams(value, entry.value, server.id, ServerModel.Url.value.iri(), ctx)
        if (!TemplateUri.isValid(value))
          ctx.violation(InvalidServerPath, api.id, TemplateUri.invalidMsg(value), entry.value)

        map.key("serverDescription".asRamlAnnotation, ServerModel.Description in server)

        parseBaseUriParameters(server, TemplateUri.variables(value))

        api.set(WebApiModel.Servers,
                AmfArray(Seq(server.add(SynthesizedField())), Annotations(entry.value)),
                Annotations(entry))
      case None =>
        map
          .key("baseUriParameters")
          .foreach { entry =>
            ctx.violation(ParametersWithoutBaseUri,
                          api.id,
                          "'baseUri' not defined and 'baseUriParameters' defined.",
                          entry)

            val server = Server().adopted(api.id)
            parseBaseUriParameters(server, Nil)

            api.set(WebApiModel.Servers,
                    AmfArray(Seq(server.add(SynthesizedField())), Annotations(entry.value)),
                    Annotations(entry))
          }
    }

    map.key("servers".asRamlAnnotation).foreach { entry =>
      entry.value.as[Seq[YMap]].map(OasServerParser(api.id, _)(toOas(ctx)).parse()).foreach { server =>
        api.add(WebApiModel.Servers, server)
      }
    }
  }

  private def parseBaseUriParameters(server: Server, orderedVariables: Seq[String]): Unit = {
    val maybeEntry = map.key("baseUriParameters")
    maybeEntry match {
      case Some(entry) =>
        entry.value.tagType match {
          case YType.Map =>
            val parameters =
              RamlParametersParser(entry.value.as[YMap], (p: Parameter) => p.adopted(server.id))
                .parse()
                .map(_.withBinding("path"))

            val flatten: Seq[Parameter] = orderedVariables.map(v =>
              parameters.find(_.name.value().equals(v)) match {
                case Some(p) => p
                case _       => buildParamFromVar(v, server.id)

            })
            val (_, unused) = parameters.partition(flatten.contains(_))
            val finalParams = flatten ++ unused
            server.set(ServerModel.Variables, AmfArray(finalParams, Annotations(entry.value)), Annotations(entry))
            unused.foreach { p =>
              ctx.warning(UnusedBaseUriParameter,
                          p.id,
                          None,
                          s"Unused base uri parameter ${p.name.value()}",
                          p.position(),
                          p.location())
            }
          case YType.Null =>
          case _          => ctx.violation(InvalidBaseUriParametersType, "", "Invalid node for baseUriParameters", entry.value)
        }
      case None =>
        if (orderedVariables.nonEmpty)
          server.set(ServerModel.Variables,
                     AmfArray(orderedVariables.map(buildParamFromVar(_, server.id)), Annotations()),
                     Annotations())
    }

  }

  private def buildParamFromVar(v: String, serverId: String) = {
    val param = Parameter().withName(v).withBinding("path").withRequired(true)
    param.adopted(serverId)
    param.withScalarSchema(v).withDataType(DataType.String)
    param.annotations += SynthesizedField()
    param
  }
}

abstract class OasServersParser(map: YMap, api: WebApi)(implicit val ctx: OasWebApiContext) extends SpecParserOps {
  def parse(): Unit

  protected def parseServers(key: String): Unit =
    map.key(key).foreach { entry =>
      entry.value.as[Seq[YMap]].map(OasServerParser(api.id, _).parse()).foreach { server =>
        api.add(WebApiModel.Servers, server)
      }
    }
}

case class Oas3ServersParser(map: YMap, api: WebApi)(implicit override val ctx: OasWebApiContext)
    extends OasServersParser(map, api) {

  override def parse(): Unit = parseServers("servers")
}

case class Oas2ServersParser(map: YMap, api: WebApi)(implicit override val ctx: OasWebApiContext)
    extends OasServersParser(map, api) {
  override def parse(): Unit = {
    if (baseUriExists(map)) {
      var host     = ""
      var basePath = ""

      val annotations = Annotations()

      map.key("basePath").foreach { entry =>
        annotations += BasePathLexicalInformation(Range(entry.range))
        basePath = entry.value.as[String]

        if (!basePath.startsWith("/")) {
          ctx.violation(InvalidBasePath, api.id, "'basePath' property must start with '/'", entry.value)
          basePath = "/" + basePath
        }
      }
      map.key("host").foreach { entry =>
        annotations += HostLexicalInformation(Range(entry.range))
        host = entry.value.as[String]
      }

      val server = Server().set(ServerModel.Url, AmfScalar(host + basePath), annotations)

      map.key("serverDescription".asOasExtension, ServerModel.Description in server)

      map.key(
        "baseUriParameters".asOasExtension,
        entry => {
          val uriParameters =
            RamlParametersParser(entry.value.as[YMap], (p: Parameter) => p.adopted(server.id))(toRaml(ctx))
              .parse()
              .map(_.withBinding("path"))

          server.set(ServerModel.Variables, AmfArray(uriParameters, Annotations(entry.value)), Annotations(entry))
        }
      )

      api.set(WebApiModel.Servers, AmfArray(Seq(server.add(SynthesizedField())), Annotations()))
    }

    parseServers("servers".asOasExtension)
  }

  def baseUriExists(map: YMap): Boolean = map.key("host").orElse(map.key("basePath")).isDefined
}

private case class OasServerParser(parent: String, map: YMap)(implicit val ctx: OasWebApiContext)
    extends SpecParserOps {
  def parse(): Server = {
    val server = Server()

    map.key("url", ServerModel.Url in server)

    server.adopted(parent)

    map.key("description", ServerModel.Description in server)

    map.key("parameters".asOasExtension).orElse(map.key("variables")).foreach { entry =>
      val variables = entry.value
        .as[YMap]
        .entries
        .map(Raml10ParameterParser(_, (p: Parameter) => p.adopted(server.id))(toRaml(ctx)).parse())

      server.set(ServerModel.Variables, AmfArray(variables, Annotations(entry.value)), Annotations(entry))
    }

    AnnotationParser(server, map).parse()

    server
  }
}

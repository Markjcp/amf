package amf.plugins.document.webapi.parser

import amf.core.parser.ParsedReference
import amf.core.remote.Vendor
import amf.plugins.document.webapi.JsonSchemaWebApiContext
import amf.plugins.document.webapi.contexts._

/**
  * Oas package object
  */
package object spec {

  object OasDefinitions {
    val definitionsPrefix = "#/definitions/"

    val oas3ComponentsPrefix = "#/components/"

    val parameterDefinitionsPrefix = "#/parameters/"

    val responsesDefinitionsPrefix = "#/responses/"

    def stripDefinitionsPrefix(url: String): String = url.stripPrefix(definitionsPrefix)

    def stripParameterDefinitionsPrefix(url: String)(implicit ctx: WebApiContext): String = {
      if (ctx.vendor == Vendor.OAS30)
        stripOas3ComponentsPrefix(url, "parameters")
      else
        url.stripPrefix(parameterDefinitionsPrefix)
    }

    def stripOas3ComponentsPrefix(url: String, fieldName: String): String =
      url.stripPrefix(oas3ComponentsPrefix + fieldName + "/")

    def stripResponsesDefinitionsPrefix(url: String)(implicit ctx: OasWebApiContext): String = {
      if (ctx.vendor == Vendor.OAS30)
        stripOas3ComponentsPrefix(url, "responses")
      else
        url.stripPrefix(responsesDefinitionsPrefix)
    }

    def appendDefinitionsPrefix(url: String): String =
      if (!url.startsWith(definitionsPrefix)) appendPrefix(definitionsPrefix, url) else url

    def appendParameterDefinitionsPrefix(url: String, asHeader: Boolean = false)(
        implicit spec: OasSpecEmitterContext): String = {
      if (spec.factory.isInstanceOf[Oas3SpecEmitterFactory])
        appendOas3ComponentsPrefix(url, "parameters")
      else
        appendPrefix(parameterDefinitionsPrefix, url)
    }

    def appendResponsesDefinitionsPrefix(url: String)(implicit spec: OasSpecEmitterContext): String = {
      if (spec.factory.isInstanceOf[Oas3SpecEmitterFactory])
        appendOas3ComponentsPrefix(url, "responses")
      else
        appendPrefix(responsesDefinitionsPrefix, url)
    }

    def appendOas3ComponentsPrefix(url: String, fieldName: String): String = {
      appendPrefix(oas3ComponentsPrefix + s"$fieldName/", url)
    }

    private def appendPrefix(prefix: String, url: String): String = prefix + url
  }

  // TODO oas2? raml10?
  def toOas(ctx: WebApiContext): OasWebApiContext = {
    ctx.vendor match {
      case Vendor.OAS30 =>
        new Oas3WebApiContext(ctx.rootContextDocument,
                              ctx.refs,
                              ctx,
                              Some(toOasDeclarations(ctx.declarations)),
                              Some(ctx.parserCount),
                              ctx.eh,
                              ctx.options)
      case _ =>
        new Oas2WebApiContext(ctx.rootContextDocument,
                              ctx.refs,
                              ctx,
                              Some(toOasDeclarations(ctx.declarations)),
                              Some(ctx.parserCount),
                              ctx.eh,
                              ctx.options)
    }

  }

  def toOas(root: String, refs: Seq[ParsedReference], ctx: WebApiContext): OasWebApiContext = {
    new Oas2WebApiContext(root,
                          refs,
                          ctx,
                          Some(toOasDeclarations(ctx.declarations)),
                          Some(ctx.parserCount),
                          ctx.eh,
                          ctx.options)
  }

  def toRaml(ctx: WebApiContext): RamlWebApiContext = {
    new Raml10WebApiContext(ctx.rootContextDocument,
                            ctx.refs,
                            ctx,
                            Some(toRamlDeclarations(ctx.declarations)),
                            Some(ctx.parserCount),
                            ctx.eh,
                            options = ctx.options)
  }

  private def toRamlDeclarations(ds: WebApiDeclarations) = {
    ds match {
      case raml: RamlWebApiDeclarations => raml
      case other                        => RamlWebApiDeclarations(other)
    }
  }

  def toOasDeclarations(ds: WebApiDeclarations): OasWebApiDeclarations = {
    ds match {
      case oas: OasWebApiDeclarations => oas
      case other                      => OasWebApiDeclarations(other)
    }
  }

  def toRaml(spec: SpecEmitterContext): RamlSpecEmitterContext = {
    new Raml10SpecEmitterContext(spec.eh, spec.getRefEmitter)
  }

  def toOas(spec: SpecEmitterContext): OasSpecEmitterContext = {
    new Oas2SpecEmitterContext(spec.eh, spec.getRefEmitter)
  }

  def toJsonSchema(ctx: WebApiContext): JsonSchemaWebApiContext = {
    new JsonSchemaWebApiContext(ctx.rootContextDocument,
                                ctx.refs,
                                ctx,
                                Some(toOasDeclarations(ctx.declarations)),
                                Some(ctx.parserCount),
                                ctx.eh,
                                ctx.options)
  }

  def toJsonSchema(root: String, refs: Seq[ParsedReference], ctx: WebApiContext): OasWebApiContext = {
    new JsonSchemaWebApiContext(root,
                                refs,
                                ctx,
                                Some(toOasDeclarations(ctx.declarations)),
                                Some(ctx.parserCount),
                                ctx.eh,
                                ctx.options)
  }
}

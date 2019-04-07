package amf.plugins.document.webapi.parser.spec.declaration

import amf.core.Root
import amf.core.annotations.Aliases
import amf.core.model.document.{BaseUnit, DeclaresModel, Document, Fragment}
import amf.core.parser.{ParsedReference, _}
import amf.plugins.document.webapi.contexts.WebApiContext
import amf.plugins.features.validation.ParserSideValidations
import amf.plugins.features.validation.ParserSideValidations.{ExpectedModule, InvalidModuleType}
import org.yaml.model.{YMap, YMapEntry, YScalar, YType}

import scala.collection.mutable

/**
  *
  */
object ReferenceDeclarations {
  def apply(references: mutable.Map[String, BaseUnit])(implicit ctx: WebApiContext) =
    new ReferenceDeclarations(references)

  def apply()(implicit ctx: WebApiContext): ReferenceDeclarations = apply(mutable.Map[String, BaseUnit]())
}

case class ReferenceDeclarations(references: mutable.Map[String, BaseUnit] = mutable.Map())(
    implicit ctx: WebApiContext) {

  def +=(alias: String, unit: BaseUnit): Unit = {
    references += (alias -> unit)
    val library = ctx.webApiDeclarations.getOrCreateLibrary(alias)
    // todo : ignore domain entities of vocabularies?
    unit match {
      case d: DeclaresModel =>
        d.declares.foreach(library += _)
    }
  }

  def +=(url: String, fragment: Fragment): Unit = {
    references += (url       -> fragment)
    ctx.webApiDeclarations += (url -> fragment)
  }

  def +=(url: String, fragment: Document): Unit = references += (url -> fragment)

  def solvedReferences(): Seq[BaseUnit] = references.values.toSet.toSeq
}

case class ReferencesParser(baseUnit: BaseUnit, key: String, map: YMap, references: Seq[ParsedReference])(
    implicit ctx: WebApiContext) {
  def parse(location: String): ReferenceDeclarations = {
    val result: ReferenceDeclarations = parseLibraries(location)

    references.foreach {
      case ParsedReference(f: Fragment, origin: Reference, _) => result += (origin.url, f)
      case ParsedReference(d: Document, origin: Reference, _) => result += (origin.url, d)
      case _                                                  =>
    }

    result
  }

  private def target(url: String): Option[BaseUnit] =
    references.find(r => r.origin.url.equals(url)).map(_.unit)

  private def parseLibraries(id: String): ReferenceDeclarations = {
    val result = ReferenceDeclarations()

    map.key(
      key,
      entry =>
        entry.value.tagType match {
          case YType.Map =>
            entry.value
              .as[YMap]
              .entries
              .foreach(e => {
                val alias: String = e.key.as[YScalar].text
                val urlOption     = LibraryLocationParser(e, ctx)
                urlOption.foreach { url =>
                  target(url).foreach {
                    case module: DeclaresModel =>
                      collectAlias(baseUnit, alias -> (module.id, url))
                      result += (alias, module)
                    case other =>
                      ctx
                        .violation(ExpectedModule, id, s"Expected module but found: $other", e)
                  }
                }
              })
          case YType.Null =>
          case _ =>
            ctx.violation(InvalidModuleType, id, s"Invalid ast type for uses: ${entry.value.tagType}", entry.value)
      }
    )

    result
  }

  private def library(id: String, e: YMapEntry): String = e.value.tagType match {
    case YType.Include => e.value.as[YScalar].text
    case _             => e.value
  }

  private def collectAlias(module: BaseUnit,
                           alias: (Aliases.Alias, (Aliases.FullUrl, Aliases.RelativeUrl))): BaseUnit = {
    module.annotations.find(classOf[Aliases]) match {
      case Some(aliases) =>
        module.annotations.reject(_.isInstanceOf[Aliases])
        module.add(aliases.copy(aliases = aliases.aliases + alias))
      case None => module.add(Aliases(Set(alias)))
    }
  }
}

// Helper method to parse references and annotations before having an actual base unit
object ReferencesParserAnnotations {
  def apply(key: String, map: YMap, root: Root)(
      implicit ctx: WebApiContext): (ReferenceDeclarations, Option[Aliases]) = {
    val tmp          = Document()
    val declarations = ReferencesParser(tmp, key, map, root.references).parse(root.location)
    (declarations, tmp.annotations.find(classOf[Aliases]))
  }
}

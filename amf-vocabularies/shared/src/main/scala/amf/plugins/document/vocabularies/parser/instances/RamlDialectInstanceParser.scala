package amf.plugins.document.vocabularies.parser.instances

import amf.core.Root
import amf.core.annotations.{Aliases, LexicalInformation}
import amf.core.model.document.{BaseUnit, DeclaresModel, EncodesModel}
import amf.core.model.domain.Annotation
import amf.core.parser.{Annotations, BaseSpecParser, Declarations, EmptyFutureDeclarations, ErrorHandler, FutureDeclarations, ParsedReference, ParserContext, Reference, SearchScope, _}
import amf.core.utils._
import amf.core.vocabulary.Namespace
import amf.plugins.document.vocabularies.RAMLVocabulariesPlugin
import amf.plugins.document.vocabularies.annotations.{AliasesLocation, CustomId}
import amf.plugins.document.vocabularies.model.document.{Dialect, DialectInstance, DialectInstanceFragment, DialectInstanceLibrary}
import amf.plugins.document.vocabularies.model.domain._
import amf.plugins.document.vocabularies.parser.common.SyntaxErrorReporter
import amf.plugins.features.validation.ParserSideValidations
import org.yaml.model._

import scala.collection.mutable

class DialectInstanceDeclarations(var dialectDomainElements: Map[String, DialectDomainElement] = Map(),
                                  errorHandler: Option[ErrorHandler],
                                  futureDeclarations: FutureDeclarations)
  extends Declarations(Map(), Map(), Map(), errorHandler, futureDeclarations) {

  /** Get or create specified library. */
  override def getOrCreateLibrary(alias: String): DialectInstanceDeclarations = {
    libraries.get(alias) match {
      case Some(lib: DialectInstanceDeclarations) => lib
      case _ =>
        val result = new DialectInstanceDeclarations(errorHandler = errorHandler, futureDeclarations = EmptyFutureDeclarations())
        libraries = libraries + (alias -> result)
        result
    }
  }

  def registerDialectDomainElement(name: String, dialectDomainElement: DialectDomainElement): DialectInstanceDeclarations = {
    dialectDomainElements += (name -> dialectDomainElement)
    if (!dialectDomainElement.isUnresolved) {
      futureDeclarations.resolveRef(name, dialectDomainElement)
    }
    this
  }

  def findDialectDomainElement(key: String, nodeMapping: NodeMapping, scope: SearchScope.Scope): Option[DialectDomainElement] = {
    findForType(key, _.asInstanceOf[DialectInstanceDeclarations].dialectDomainElements, scope) collect {
      case dialectDomainElement: DialectDomainElement if dialectDomainElement.definedBy.id == nodeMapping.id => dialectDomainElement
    }
  }

  override def declarables: Seq[DialectDomainElement] = dialectDomainElements.values.toSeq
}


class DialectInstanceContext(var dialect: Dialect, private val wrapped: ParserContext, private val ds: Option[DialectInstanceDeclarations] = None)
  extends ParserContext(wrapped.rootContextDocument, wrapped.refs, wrapped.futureDeclarations, wrapped.parserCount) with SyntaxErrorReporter {

  var nestedDialects: Seq[Dialect] = Nil
  val libraryDeclarationsNodeMappings: Map[String, NodeMapping] = parseDeclaredNodeMappings("library")
  val rootDeclarationsNodeMappings: Map[String, NodeMapping]    = parseDeclaredNodeMappings("root")

  val declarations: DialectInstanceDeclarations =
    ds.getOrElse(new DialectInstanceDeclarations(errorHandler = Some(this), futureDeclarations = futureDeclarations))

  def withCurrentDialect[T](tmpDialect: Dialect)(k: => T) = {
    val oldDialect = dialect
    dialect = tmpDialect
    val res = k
    dialect = oldDialect
    res
  }

  protected def parseDeclaredNodeMappings(documentType: String): Map[String, NodeMapping] = {
    val declarations: Seq[(String, NodeMapping)] = Option(dialect.documents()).flatMap { documents =>
      // document mappings for root and libraries, everything that declares something
      val documentMappings: Option[DocumentMapping] = if (documentType == "root") {
        Option(documents.root())
      } else {
        Option(documents.library())
      }
      documentMappings.map { mapping =>
        mapping.declaredNodes() map { declaration: PublicNodeMapping =>
          findNodeMapping(declaration.mappedNode().value()) map { nodeMapping =>
            (declaration.name().value(), nodeMapping)
          }
        } collect { case Some(res: (String, NodeMapping)) => res }
      }
    }.getOrElse(Nil)

    declarations.foldLeft(Map[String, NodeMapping]()) { case (acc, (name, mapping)) =>
      acc + (name -> mapping)
    }
  }

  def findNodeMapping(mappingId: String): Option[NodeMapping] = {
    dialect.declares.collectFirst {
      case mapping: NodeMapping if mapping.id == mappingId => mapping
    }
  }

  private def isInclude(node: YNode) = node.tagType == YType.Include

  def link(node: YNode): Either[String, YNode] = {
    node match {
      case _ if isInclude(node) => Left(node.as[YScalar].text)
      case _                    => Right(node)
    }
  }
}

case class ReferenceDeclarations(references: mutable.Map[String, Any] = mutable.Map())(implicit ctx: DialectInstanceContext) {
  def +=(alias: String, unit: BaseUnit): Unit = {
    references += (alias -> unit)
    unit match {
      case m: DeclaresModel =>
        val library = ctx.declarations.getOrCreateLibrary(alias)
        m.declares.foreach {
          case dialectElement: DialectDomainElement =>
            val localName = dialectElement.localRefName
            library.registerDialectDomainElement(localName, dialectElement)
            ctx.futureDeclarations.resolveRef(s"$alias.$localName", dialectElement)
          case decl                                 => library += decl
        }
      case f: DialectInstanceFragment =>
        ctx.declarations.fragments += (alias -> f.encodes)
    }
  }

  def baseUnitReferences(): Seq[BaseUnit] =
    references.values.toSet.filter(_.isInstanceOf[BaseUnit]).toSeq.asInstanceOf[Seq[BaseUnit]]
}

case class DialectInstanceReferencesParser(dialectInstance: BaseUnit, map: YMap, references: Seq[ParsedReference])(implicit ctx: DialectInstanceContext) {

  def parse(location: String): ReferenceDeclarations = {
    val result = ReferenceDeclarations()
    parseLibraries(dialectInstance, result, location)
    references.foreach {
      case ParsedReference(f: DialectInstanceFragment, origin: Reference, None) => result += (origin.url, f)
      case _                                                                    =>
    }

    result
  }

  private def target(url: String): Option[BaseUnit] =
    references.find(r => r.origin.url.equals(url)).map(_.unit)


  private def parseLibraries(dialectInstance: BaseUnit, result: ReferenceDeclarations, id: String): Unit = {
    map.key(
      "uses",
      entry => {
        val annotation: Annotation = AliasesLocation(Annotations(entry.key).find(classOf[LexicalInformation]).map(_.range.start.line).getOrElse(0))
        dialectInstance.annotations += annotation
        entry.value
          .as[YMap]
          .entries
          .foreach(e => {
            val alias: String = e.key
            val url: String = library(e)
            target(url).foreach {
              case module: DeclaresModel =>
                collectAlias(dialectInstance, alias -> module.id)
                result += (alias, module)
              case other =>
                ctx.violation(id, s"Expected vocabulary module but found: $other", e) // todo Uses should only reference modules...
            }
          })
      }
    )
  }

  private def library(e: YMapEntry): String = e.value.tagType match {
    case YType.Include => e.value.as[YScalar].text
    case _             => e.value
  }


  private def collectAlias(aliasCollectorUnit: BaseUnit, alias: (String, String)): BaseUnit = {
    aliasCollectorUnit.annotations.find(classOf[Aliases]) match {
      case Some(aliases) =>
        aliasCollectorUnit.annotations.reject(_.isInstanceOf[Aliases])
        aliasCollectorUnit.add(aliases.copy(aliases = aliases.aliases + alias))
      case None => aliasCollectorUnit.add(Aliases(Set(alias)))
    }
  }
}


class RamlDialectInstanceParser(root: Root)(implicit override val ctx: DialectInstanceContext) extends BaseSpecParser {
  val map: YMap = root.parsed.document.as[YMap]


  def parseDocument(): Option[DialectInstance] = {
    val dialectInstance: DialectInstance = DialectInstance(Annotations(map)).withLocation(root.location).withId(root.location + "#").withDefinedBy(ctx.dialect.id)
    parseDeclarations("root")
    val references = DialectInstanceReferencesParser(dialectInstance, map, root.references).parse(dialectInstance.location)

    val document = parseEncoded(dialectInstance) match {

      case Some(dialectDomainElement) =>
        dialectInstance.withEncodes(dialectDomainElement)
        if (ctx.declarations.declarables.nonEmpty)
          dialectInstance.withDeclares(ctx.declarations.declarables)
        if (references.baseUnitReferences().nonEmpty)
          dialectInstance.withReferences(references.baseUnitReferences())
        if (ctx.nestedDialects.nonEmpty)
          dialectInstance.withGraphDependencies(ctx.nestedDialects.map(_.id))
        Some(dialectInstance)

      case _ => None

    }

    // resolve unresolved references
    ctx.futureDeclarations.resolve()

    document
  }

  def parseFragment(): Option[DialectInstanceFragment] = {
    val dialectInstanceFragment: DialectInstanceFragment = DialectInstanceFragment(Annotations(map)).withLocation(root.location).withId(root.location + "#").withDefinedBy(ctx.dialect.id)
    parseEncodedFragment(dialectInstanceFragment) match {
      case Some(dialectDomainElement) => Some(dialectInstanceFragment.withEncodes(dialectDomainElement))
      case _                          => None
    }
  }

  def parseLibrary(): Option[DialectInstanceLibrary] = {
    val dialectInstance: DialectInstanceLibrary = DialectInstanceLibrary(Annotations(map)).withLocation(root.location).withId(root.location + "#").withDefinedBy(ctx.dialect.id)

    parseDeclarations("library")

    val references = DialectInstanceReferencesParser(dialectInstance, map, root.references).parse(dialectInstance.location)

    if (ctx.declarations.declarables.nonEmpty)
      dialectInstance.withDeclares(ctx.declarations.declarables)

    if (references.baseUnitReferences().nonEmpty)
      dialectInstance.withReferences(references.baseUnitReferences())

    // resolve unresolved references
    ctx.futureDeclarations.resolve()

    Some(dialectInstance)
  }

  protected def parseDeclarations(documentType: String): Unit = {
    val declarationsNodeMappings = if (documentType == "root") {
      ctx.rootDeclarationsNodeMappings
    } else {
      ctx.libraryDeclarationsNodeMappings
    }
    declarationsNodeMappings.foreach { case (name, nodeMapping) =>
        map.entries.find(_.key.as[String] == name).foreach { entry =>
          val declarationsId = root.location + "#/" + name.urlEncoded
          entry.value.as[YMap].entries.foreach { declarationEntry =>
            val id = declarationsId + "/" + declarationEntry.key.as[String].urlEncoded
            parseNode(id, declarationEntry.value, nodeMapping) match {
              case Some(node) => ctx.declarations.registerDialectDomainElement(declarationEntry.key, node)
              case other      => // TODO: violation here
            }
          }
        }
    }
  }

  protected def parseEncoded(dialectInstance: EncodesModel): Option[DialectDomainElement] = {
    Option(ctx.dialect.documents()) flatMap {
      documents: DocumentsModel =>
        Option(documents.root()) flatMap {
          mapping =>
            ctx.findNodeMapping(mapping.encoded().value()) match {
              case Some(nodeMapping) => parseNode(dialectInstance.id + "/", map, nodeMapping)
              case _ => None
            }
        }
    }
  }

  protected def parseEncodedFragment(dialectInstanceFragment: DialectInstanceFragment): Option[DialectDomainElement] = {
    Option(ctx.dialect.documents()) flatMap {
      documents: DocumentsModel =>
        Option(documents.fragments()).getOrElse(Nil).find { documentMapping =>
          root.parsed.comment.get.metaText.replace(" ", "").contains(documentMapping.documentName().value())
        } match {
          case Some(documentMapping) =>
            ctx.findNodeMapping(documentMapping.encoded().value()) match {
              case Some(nodeMapping) => parseNode(dialectInstanceFragment.id + "/", map, nodeMapping)
              case _ => None
            }
          case None => None
        }
    }
  }

  def parseProperty(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    property.classification() match {
      case ExtensionPointProperty       => parseDialectExtension(id, propertyEntry, property, node)
      case LiteralProperty              => parseLiteralProperty(id, propertyEntry, property, node)
      case LiteralPropertyCollection    => parseLiteralCollectionProperty(id, propertyEntry, property, node)
      case ObjectProperty               => parseObjectProperty(id, propertyEntry, property, node)
      case ObjectPropertyCollection     => parseObjectCollectionProperty(id, propertyEntry, property, node)
      case ObjectMapProperty            => parseObjectMapProperty(id, propertyEntry,property, node)
      case ObjectPairProperty           => parseObjectPairProperty(id, propertyEntry,property, node)
      case _ => // TODO: throw exception
    }
  }

  def parseDialectExtension(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    propertyEntry.value.tagType match {
      case YType.Str => // TODO: support external link here
      case YType.Map =>
        val map = propertyEntry.value.as[YMap]
        map.key("$dialect") match {
          case Some(nested) if nested.value.tagType == YType.Str =>
            val dialectNode = nested.value.as[String]
            // TODO: resolve dialect node URI to absolute normalised URI
            RAMLVocabulariesPlugin.registry.findNode(dialectNode) match {
              case Some((dialect, nodeMapping)) =>
                ctx.nestedDialects ++= Seq(dialect)
                ctx.withCurrentDialect(dialect) {
                  val nestedObjectId = pathSegment(id, propertyEntry.key.as[String].urlEncoded)
                  parseNestedNode(nestedObjectId, propertyEntry.value, nodeMapping) match {
                    case Some(dialectDomainElement) => node.setObjectField(property, dialectDomainElement, propertyEntry.value)
                    case None => // ignore
                  }
                }
              case None =>
                ctx.violation(ParserSideValidations.ParsingErrorSpecification.id(), id, s"Cannot find nested node mapping $dialectNode", nested.value)
            }
          case None =>
            ctx.violation(ParserSideValidations.ParsingErrorSpecification.id(), id, "$dialect key without string value", map)
        }
    }
  }

  def checkHashProperties(node: DialectDomainElement, propertyMapping: PropertyMapping, propertyEntry: YMapEntry): DialectDomainElement = {
    // TODO: check if the node already has a value and that it matches (maybe coming from a declaration)
    propertyMapping.mapKeyProperty().option() match {
        case Some(propId) => node.setMapKeyField(propId, propertyEntry.key.as[String], propertyEntry.key)
        case None         => node
    }
  }

  def findCompatibleMapping(unionMappings: Seq[NodeMapping], discriminatorMapping: Map[String,NodeMapping], discriminator: Option[String], nodeMap: YMap): Seq[NodeMapping] = {
    discriminator match {
        // Using explicit discriminator
      case Some(propertyName) =>
        val explicitMapping = nodeMap.entries.find(_.key.as[String] == propertyName).flatMap { entry =>
          discriminatorMapping.get(entry.value.as[String])
        }
        explicitMapping match {
          case Some(nodeMapping) => Seq(nodeMapping)
          case None              => Nil
        }
        // Inferring based on properties
      case None =>
        val properties: Set[String] = nodeMap.entries.map { entry => entry.key.as[String] }.toSet
        unionMappings.filter { mapping =>
          val mappingRequiredSet: Set[String] = Option(mapping.propertiesMapping()).map {
            props => props.filter(_.minCount().value() > 0).map(_.name().value())
          }.getOrElse(Nil).toSet
          val mappingSet: Set[String] = mapping.propertiesMapping().map { props => props.name().value() }.toSet

          // There are not additional properties in the set and all required properties are in the set
          properties.diff(mappingSet).isEmpty && mappingRequiredSet.diff(properties).isEmpty
        }
    }
  }

  def parseObjectUnion(id: String, ast: YNode, property: PropertyMapping, node: DialectDomainElement): Option[DialectDomainElement] = {
    // potential node range based in the objectRange
    val unionMappings = property.objectRange().map { nodeMappingId =>
      ctx.dialect.declares.find(_.id == nodeMappingId.value()) match {
        case Some(nodeMapping) => Some(nodeMapping)
        case None              => None // TODO: violation here
      }
    } collect { case Some(mapping: NodeMapping) => mapping }
    // potential node range based in discriminators map
    val discriminatorsMapping = Option(property.typeDiscrminator()).getOrElse(Map()).foldLeft(Map[String, NodeMapping]()) { case (acc, (alias, mappingId)) =>
        ctx.dialect.declares.find(_.id == mappingId) match {
          case Some(nodeMapping: NodeMapping) => acc + (alias -> nodeMapping)
          case _                              => acc // TODO: violation here
        }
    }
    // all possible mappings combining objectRange and type discriminator
    val allPossibleMappings = (unionMappings ++ discriminatorsMapping.values).distinct

    ast.tagType match {
      case YType.Map =>
        val nodeMap = ast.as[YMap]
        val mappings = findCompatibleMapping(unionMappings, discriminatorsMapping, property.typeDiscriminatorName().option(), nodeMap)
        if (mappings.isEmpty){
          // TODO: violation here
          None
        } else if(mappings.size == 1) {
          val node: DialectDomainElement = DialectDomainElement(nodeMap).withId(id).withDefinedBy(mappings.head)
          var instanceTypes: Seq[String] = Nil
          mappings.foreach { mapping =>
            val beforeValues = node.literalProperties.size + node.objectCollectionProperties.size + node.objectProperties.size + node.mapKeyProperties.size
            mapping.propertiesMapping().foreach { propertyMapping =>
              if (!node.containsProperty(propertyMapping)) {
                val propertyName = propertyMapping.name().value()

                nodeMap.entries.find(_.key.as[String] == propertyName) match {
                  case Some(entry) => parseProperty(id, entry, propertyMapping, node)
                  case None => // ignore
                }
              }
            }
            val afterValues = node.literalProperties.size + node.objectCollectionProperties.size + node.objectProperties.size + node.mapKeyProperties.size
            if (afterValues != beforeValues) {
              instanceTypes ++= Seq(mapping.nodetypeMapping.value())
            }
          }
          node.withInstanceTypes(instanceTypes ++ Seq(mappings.head.id))
          Some(node)
        } else {
          ctx.violation(
            ParserSideValidations.DialectAmbiguousRangeSpecification.id(),
            id,
            Some(property.nodePropertyMapping().value()),
            s"Ambiguous node, please provide a type disambiguator. Nodes ${mappings.map(_.id).mkString(",")} have been found compatible, only one is allowed",
            map)
          None
        }
      case YType.Str | YType.Include => // here the mapping information is explicit in the fragment/declaration mapping
        val refTuple = ctx.link(ast) match {
          case Left(key) =>
            (key, allPossibleMappings.map(mapping => ctx.declarations.findDialectDomainElement(key, mapping, SearchScope.Fragments)).collectFirst { case Some(x) => x })
          case _ =>
            val text = ast.as[YScalar].text
            (text, allPossibleMappings.map(mapping => ctx.declarations.findDialectDomainElement(text, mapping, SearchScope.Named)).collectFirst { case Some(x) => x })
        }
        refTuple match {
          case (text: String, Some(s)) =>
            val linkedNode = s.link(text, Annotations(ast.value))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            Some(linkedNode)
          case (text: String, _) =>
            val linkedNode = DialectDomainElement(map).withId(id)
            linkedNode.unresolved(text, map)
            Some(linkedNode)
        }

      case _ => None // TODO violation here
    }
  }

  def parseObjectProperty(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    property.nodesInRange match {
      case range: Seq[String] if range.size > 1  =>
        parseObjectUnion(id, propertyEntry.value, property, node) match {
          case Some(parsedRange) => node.setObjectField(property, parsedRange, propertyEntry.value)
          case None        => // ignore
        }
      case range: Seq[String] if range.size == 1 =>
        ctx.dialect.declares.find(_.id == range.head) match {
          case Some(nodeMapping: NodeMapping) =>
            val nestedObjectId = pathSegment(id, propertyEntry.key.as[String].urlEncoded)
            parseNestedNode(nestedObjectId, propertyEntry.value, nodeMapping) match {
              case Some(dialectDomainElement) => node.setObjectField(property, dialectDomainElement, propertyEntry.value)
              case None                       => // ignore
            }
        }
      case _ => // TODO: throw exception, illegal range
    }
  }

  def parseObjectMapProperty(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    val nested = propertyEntry.value.as[YMap].entries.map { keyEntry =>
      val nestedObjectId = pathSegment(id, propertyEntry.key.as[String].urlEncoded) + s"/${keyEntry.key.as[String].urlEncoded}"
      val parsedNode = property.nodesInRange match {
        case range: Seq[String] if range.size > 1  =>
          parseObjectUnion(nestedObjectId, keyEntry.value, property, node)
        case range: Seq[String] if range.size == 1 =>
          ctx.dialect.declares.find(_.id == range.head) match {
            case Some(nodeMapping: NodeMapping) =>
              parseNestedNode(nestedObjectId, keyEntry.value, nodeMapping)
          }
        case _ => None
      }
      parsedNode match {
        case Some(dialectDomainElement) => Some(checkHashProperties(dialectDomainElement, property, keyEntry))
        case None                       => None
      }
    }
    node.setObjectField(property, nested.collect { case Some(node: DialectDomainElement) => node }, propertyEntry.value)
  }

  def parseObjectInheritanceMap(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    val discriminator = property.typeDiscriminatorName().value()
    val nested = propertyEntry.value.as[YMap].entries.map { keyEntry =>
      val nestedObjectId = pathSegment(id, propertyEntry.key.as[String].urlEncoded) + s"/${keyEntry.key.as[String].urlEncoded}"

      val parsedNode = property.nodesInRange match {
        case range: Seq[String] if range.size > 1  =>
          parseObjectUnion(nestedObjectId, keyEntry.value, property, node)
        case range: Seq[String] if range.size == 1 =>
          ctx.dialect.declares.find(_.id == range.head) match {
            case Some(nodeMapping: NodeMapping) =>
              parseNestedNode(nestedObjectId, keyEntry.value, nodeMapping)
          }
        case _ => None
      }
      parsedNode match {
        case Some(dialectDomainElement) => Some(checkHashProperties(dialectDomainElement, property, keyEntry))
        case None                       => None
      }
    }
    node.setObjectField(property, nested.collect { case Some(node: DialectDomainElement) => node }, propertyEntry.value)
  }

  def parseObjectPairProperty(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    val propertyKeyMapping = property.mapKeyProperty().option()
    val propertyValueMapping = property.mapValueProperty().option()
    if (propertyKeyMapping.isDefined && propertyValueMapping.isDefined) {
      val nested = ctx.dialect.declares.find(_.id == property.objectRange().head.value()) match {
        case Some(nodeMapping: NodeMapping) =>
          propertyEntry.value.as[YMap].entries map { pair: YMapEntry =>
            val nestedId = id + "/" + propertyEntry.key.as[String].urlEncoded + "/" + pair.key.as[String].urlEncoded
            val nestedNode = DialectDomainElement(Annotations(pair)).withId(nestedId).withDefinedBy(nodeMapping).withInstanceTypes(Seq(nodeMapping.nodetypeMapping.value(), nodeMapping.id))
            nestedNode.setMapKeyField(propertyKeyMapping.get, pair.key.as[String], pair.key)
            nestedNode.setMapKeyField(propertyValueMapping.get, pair.value.as[String], pair.value)
            Some(nestedNode)
          } collect { case Some(elem: DialectDomainElement) => elem }
        case _ =>
          // TODO: raise violation
          Nil
      }

      node.setObjectField(property, nested, propertyEntry.key)
    } else {
      // TODO: raise violation
    }
  }

  def parseObjectCollectionProperty(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    val res = propertyEntry.value.as[YSequence].nodes.zipWithIndex.map { case (elementNode, nextElem) =>
      property.nodesInRange match {
        case range: Seq[String] if range.size > 1  =>
          parseObjectUnion(id, elementNode, property, node)
        case range: Seq[String] if range.size == 1 =>
          ctx.dialect.declares.find(_.id == range.head) match {
            case Some(nodeMapping: NodeMapping) =>
              val nestedObjectId = pathSegment(id, propertyEntry.key.as[String].urlEncoded) + s"/$nextElem"
              parseNestedNode(nestedObjectId, elementNode, nodeMapping) match {
                case Some(dialectDomainElement) => Some(dialectDomainElement)
                case None                       => None
              }
          }
        case _ => None
      }
    }
    val elems: Seq[DialectDomainElement] = res.collect { case Some(x: DialectDomainElement) => x}
    node.setObjectField(property, elems, propertyEntry.value)
  }

  def pathSegment(parent: String, next: String): String = {
    if (parent.endsWith("/")) {
      parent + next.urlEncoded
    } else {
      parent + "/" + next.urlEncoded
    }
  }

  def parseLiteralValue(value: YNode, property: PropertyMapping, node: DialectDomainElement): Option[_] = {

    value.tagType match {
      case YType.Bool if property.literalRange().value() == (Namespace.Xsd + "boolean").iri() =>
        Some(value.as[Boolean])
      case YType.Bool  =>
        ctx.inconsistentPropertyRangeValueViolation(node.id, property, property.literalRange().value(), (Namespace.Xsd + "boolean").iri(), value)
        None
      case YType.Int   if property.literalRange().value() == (Namespace.Xsd + "integer").iri() || property.literalRange().value() == (Namespace.Shapes + "number").iri() =>
        Some(value.as[Int])
      case YType.Int  =>
        ctx.inconsistentPropertyRangeValueViolation(node.id, property, property.literalRange().value(), (Namespace.Xsd + "integer").iri(), value)
        None
      case YType.Str   if property.literalRange().value() == (Namespace.Xsd + "string").iri() =>
        Some(value.as[String])
      case YType.Str  =>
        ctx.inconsistentPropertyRangeValueViolation(node.id, property, property.literalRange().value(), (Namespace.Xsd + "string").iri(), value)
        None
      case YType.Float if property.literalRange().value() == (Namespace.Xsd + "float").iri() || property.literalRange().value() == (Namespace.Shapes + "number").iri() =>
        Some(value.as[Double])
      case YType.Float  =>
        ctx.inconsistentPropertyRangeValueViolation(node.id, property, property.literalRange().value(), (Namespace.Xsd + "float").iri(), value)
        None
      case _           =>
        ctx.violation(node.id, s"Unsupported scalar type ${value.tagType}", value)
        None
    }
  }

  def setLiteralValue(value: YNode, property: PropertyMapping, node: DialectDomainElement) = {
    parseLiteralValue(value, property, node) match {
      case Some(b: Boolean) => node.setLiteralField(property, b, value)
      case Some(i: Int)     => node.setLiteralField(property, i, value)
      case Some(f: Float)   => node.setLiteralField(property, f, value)
      case Some(d: Double)  => node.setLiteralField(property, d, value)
      case Some(s: String)  => node.setLiteralField(property, s, value)
      case _                => // ignore
    }
  }

  def parseLiteralProperty(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    setLiteralValue(propertyEntry.value, property, node)
  }

  def parseLiteralCollectionProperty(id: String, propertyEntry: YMapEntry, property: PropertyMapping, node: DialectDomainElement): Unit = {
    propertyEntry.value.tagType match {
      case YType.Seq =>
        val values = propertyEntry.value.as[YSequence].nodes.map { elemValue => parseLiteralValue(elemValue, property, node) }.collect { case Some(v) => v }
        node.setLiteralField(property, values, propertyEntry.value)
      case _ =>
        parseLiteralValue(propertyEntry.value, property, node) match {
          case Some(v) => node.setLiteralField(property, Seq(v), propertyEntry.value)
          case _       => // ignore
        }
    }
  }

  protected def parseNestedNode(id: String, entry: YNode, mapping: NodeMapping): Option[DialectDomainElement] =
    parseNode(id, entry, mapping)

  protected def parseNode(id: String, ast: YNode, mapping: NodeMapping): Option[DialectDomainElement] = {
    ast.tagType match {
      case YType.Map =>
        val nodeMap = ast.as[YMap]
        val node: DialectDomainElement = DialectDomainElement(nodeMap).withDefinedBy(mapping)

        nodeMap.key("$id") match {
          case Some(entry) =>
            val rawId = entry.value.as[String]
            val externalId = if (rawId.contains("://")) {
              rawId
            } else {
              (ctx.dialect.location.split("#").head + s"#$rawId").replace("##", "#")
            }
            node.withId(externalId)
            node.annotations += CustomId()
          case None        => node.withId(id)
        }

        node.withInstanceTypes(Seq(mapping.nodetypeMapping.value(), mapping.id))
        mapping.propertiesMapping().foreach { propertyMapping =>
          val propertyName = propertyMapping.name().value()
          nodeMap.entries.find(_.key.as[String] == propertyName) match {
            case Some(entry) => parseProperty(id, entry, propertyMapping, node)
            case None        => // ignore
          }
        }
        Some(node)

      case YType.Str =>
        val refTuple = ctx.link(ast) match {
          case Left(key) =>
            (key, ctx.declarations.findDialectDomainElement(key, mapping, SearchScope.Fragments))
          case _ =>
            val text = ast.as[YScalar].text
            (text, ctx.declarations.findDialectDomainElement(text, mapping, SearchScope.Named))
        }
        refTuple match {
          case (text: String, Some(s)) =>
            val linkedNode = s.link(text, Annotations(ast.value))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            Some(linkedNode)
          case (text: String, _) =>
            val linkedNode = DialectDomainElement(map).withId(id)
            linkedNode.unresolved(text, map)
            Some(linkedNode)
        }

      case YType.Include =>
        val refTuple = ctx.link(ast) match {
          case Left(key) =>
            (key, ctx.declarations.findDialectDomainElement(key, mapping, SearchScope.Fragments))
          case _ =>
            val text = ast.as[YScalar].text
            (text, ctx.declarations.findDialectDomainElement(text, mapping, SearchScope.Named))
        }
        refTuple match {
          case (text: String, Some(s)) =>
            val linkedNode = s.link(text, Annotations(ast.value))
              .asInstanceOf[DialectDomainElement]
              .withId(id) // and the ID of the link at that position in the tree, not the ID of the linked element, tha goes in link-target
            Some(linkedNode)
          case (text: String, _) =>
            val linkedNode = DialectDomainElement(map).withId(id)
            linkedNode.unresolved(text, map)
            Some(linkedNode)
        }

      case _         => None // TODO violation here
    }
  }


}

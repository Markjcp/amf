package amf.plugins.document.webapi.parser.spec.declaration

import amf.core.annotations.ExplicitField
import amf.core.model.domain.extensions.PropertyShape
import amf.core.model.domain.{AmfArray, AmfScalar}
import amf.core.parser.{Annotations, _}
import amf.plugins.document.webapi.contexts.WebApiContext
import amf.plugins.document.webapi.parser.spec.common.SingleArrayNode
import amf.plugins.domain.shapes.metamodel.{PropertyDependenciesModel, XMLSerializerModel}
import amf.plugins.domain.shapes.models.{PropertyDependencies, XMLSerializer}
import org.yaml.model.{YMap, YMapEntry, YNode, YScalar}

import scala.collection.mutable

/**
  *
  */
case class ShapeDependenciesParser(map: YMap, properties: mutable.LinkedHashMap[String, PropertyShape])(
    implicit ctx: WebApiContext) {
  def parse(): Seq[PropertyDependencies] = {
    map.entries.flatMap(entry => NodeDependencyParser(entry, properties).parse())
  }
}

case class NodeDependencyParser(entry: YMapEntry, properties: mutable.LinkedHashMap[String, PropertyShape])(
    implicit ctx: WebApiContext) {
  def parse(): Option[PropertyDependencies] = {

    properties
      .get(entry.key.as[YScalar].text)
      .map(p => {
        PropertyDependencies(entry)
          .set(PropertyDependenciesModel.PropertySource, AmfScalar(p.id), Annotations(entry.key))
          .set(PropertyDependenciesModel.PropertyTarget, AmfArray(targets()), Annotations(entry.value))
      })
  }

  private def targets(): Seq[AmfScalar] = {
    SingleArrayNode(entry.value)
      .text()
      .scalars
      .flatMap(v => properties.get(v.value.toString).map(p => AmfScalar(p.id, v.annotations)))
  }
}

object XMLSerializerParser {
  def parse(defaultName: String)(node: YNode)(implicit ctx: WebApiContext): XMLSerializer =
    XMLSerializerParser(defaultName, node).parse()
}

case class XMLSerializerParser(defaultName: String, node: YNode)(implicit ctx: WebApiContext) {
  val map: YMap = node.as[YMap]
  def parse(): XMLSerializer = {
    val serializer = XMLSerializer(node)
      .set(XMLSerializerModel.Attribute, value = false)
      .set(XMLSerializerModel.Wrapped, value = false)

    map.key(
      "attribute",
      entry => {
        val value = ScalarNode(entry.value)
        serializer.set(XMLSerializerModel.Attribute, value.boolean(), Annotations(entry) += ExplicitField())
      }
    )

    map.key("wrapped", entry => {
      val value = ScalarNode(entry.value)
      serializer.set(XMLSerializerModel.Wrapped, value.boolean(), Annotations(entry) += ExplicitField())
    })

    map.key("name", entry => {
      val value = ScalarNode(entry.value)
      serializer.set(XMLSerializerModel.Name, value.string(), Annotations(entry) += ExplicitField())
    })

    map.key("namespace", entry => {
      val value = ScalarNode(entry.value)
      serializer.set(XMLSerializerModel.Namespace, value.string(), Annotations(entry))
    })

    map.key("prefix", entry => {
      val value = ScalarNode(entry.value)
      serializer.set(XMLSerializerModel.Prefix, value.string(), Annotations(entry))
    })

    ctx.closedShape(serializer.id, map, "xmlSerialization")

    serializer
  }
}

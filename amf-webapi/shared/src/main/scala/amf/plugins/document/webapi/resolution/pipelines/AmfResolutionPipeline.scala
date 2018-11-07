package amf.plugins.document.webapi.resolution.pipelines

import amf.{AmfProfile, ProfileName}
import amf.core.model.document.BaseUnit
import amf.core.resolution.pipelines.ResolutionPipeline
import amf.core.resolution.stages._
import amf.plugins.document.webapi.resolution.stages.ExtensionsResolutionStage
import amf.plugins.domain.shapes.resolution.stages.ShapeNormalizationStage
import amf.plugins.domain.webapi.resolution.stages._

class AmfResolutionPipeline(override val model: BaseUnit) extends ResolutionPipeline[BaseUnit] {
  override def profileName: ProfileName = AmfProfile

  protected lazy val references = new ReferenceResolutionStage(keepEditingInfo = false)

  override protected val steps: Seq[ResolutionStage] = Seq(
    references,
    new ExternalSourceRemovalStage,
    new ExtensionsResolutionStage(profileName, keepEditingInfo = false),
    new ShapeNormalizationStage(profileName, keepEditingInfo = false),
    new SecurityResolutionStage(),
    new ParametersNormalizationStage(profileName),
    new MediaTypeResolutionStage(profileName),
    new ExamplesResolutionStage(),
    new CleanReferencesStage(),
    new DeclarationsRemovalStage()
  )

}

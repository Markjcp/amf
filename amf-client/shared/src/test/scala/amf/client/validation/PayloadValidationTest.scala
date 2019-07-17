package amf.client.validation

import amf.client.convert.NativeOps
import amf.client.model.DataTypes
import amf.client.model.domain.ScalarShape
import amf.core.AMF
import amf.plugins.document.webapi.validation.PayloadValidatorPlugin
import org.scalatest.AsyncFunSuite

import scala.concurrent.ExecutionContext

trait ClientPayloadValidationTest extends AsyncFunSuite with NativeOps {

  test("Test parameter validator int payload") {
    AMF.init().flatMap { _ =>
      amf.Core.registerPlugin(PayloadValidatorPlugin)

      val test = new ScalarShape().withDataType(DataTypes.String).withName("test")

      test
        .parameterValidator("application/yaml")
        .asOption
        .get
        .validate("application/yaml", "1234")
        .asFuture
        .map(r => assert(r.conforms))
    }
  }

  test("Test parameter validator boolean payload") {
    AMF.init().flatMap { _ =>
      amf.Core.registerPlugin(PayloadValidatorPlugin)

      val test = new ScalarShape().withDataType(DataTypes.String).withName("test")

      test
        .parameterValidator("application/yaml")
        .asOption
        .get
        .validate("application/yaml", "true")
        .asFuture
        .map(r => assert(r.conforms))
    }
  }

  override implicit def executionContext: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
}

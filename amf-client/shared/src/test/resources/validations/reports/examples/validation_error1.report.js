Model: file://amf-client/shared/src/test/resources/validations/08/validation_error1.raml
Profile: RAML 0.8
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#example-validation-error
  Message: should be object
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/08/validation_error1.raml#/web-api/end-points/%2Freservations%2F%7Bpnrcreationdate%7D/get/200/application%2Fjson/application%2Fjson/examples/example/default-example
  Property: 
  Position: Some(LexicalInformation([(27,26)-(28,77)]))
  Location: file://amf-client/shared/src/test/resources/validations/08/validation_error1.raml

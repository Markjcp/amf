Model: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api3.raml
Profile: RAML 1.0
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#example-validation-error
  Message: [1] should be integer
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api3.raml#/web-api/end-points/%2Fep2/get/200/application%2Fjson/any/schema/example/default-example
  Property: 
  Position: Some(LexicalInformation([(29,21)-(29,31)]))
  Location: file://amf-client/shared/src/test/resources/validations/jsonschema/ref/api3.raml

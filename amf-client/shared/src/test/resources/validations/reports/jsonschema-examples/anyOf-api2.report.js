Model: file://amf-client/shared/src/test/resources/validations/jsonschema/anyOf/api2.raml
Profile: RAML 1.0
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/validation#example-validation-error
  Message: should NOT be longer than 2 characters
should NOT be shorter than 4 characters
should match some schema in anyOf

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/jsonschema/anyOf/api2.raml#/web-api/end-points/%2Fep3/get/200/application%2Fjson/scalar/schema/example/default-example
  Property: file://amf-client/shared/src/test/resources/validations/jsonschema/anyOf/api2.raml#/web-api/end-points/%2Fep3/get/200/application%2Fjson/scalar/schema/example/default-example
  Position: Some(LexicalInformation([(43,21)-(43,24)]))
  Location: file://amf-client/shared/src/test/resources/validations/jsonschema/anyOf/api2.raml

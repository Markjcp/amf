Model: file://amf-client/shared/src/test/resources/validations/enums/invalid-obj-enum.raml
Profile: RAML
Conforms? false
Number of results: 1

Level: Violation

- Source: http://a.ml/vocabularies/amf/parser#exampleError
  Message: should have required property 'value1'
should have required property 'value2'

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/validations/enums/invalid-obj-enum.raml#/declarations/types/A/object_1
  Property: file://amf-client/shared/src/test/resources/validations/enums/invalid-obj-enum.raml#/declarations/types/A/object_1
  Position: Some(LexicalInformation([(9,8)-(9,13)]))
  Location: file://amf-client/shared/src/test/resources/validations/enums/invalid-obj-enum.raml
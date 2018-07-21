Model: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/object/input.raml
Profile: RAML
Conforms? false
Number of results: 2

Level: Violation

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/object/input.raml#/declarations/annotations/user/schema_validation_alive_validation_minCount/prop
  Message: Data at //alive must have min. cardinality 1
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/object/input.raml#/web-api/end-points/%2Fbad0/user/object_1
  Property: http://a.ml/vocabularies/data#alive
  Position: Some(LexicalInformation([(22,0)-(23,0)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/object/input.raml

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/object/input.raml#/declarations/annotations/user/schema_validation
  Message: Object at / must be valid
Scalar at //alive must have data type http://www.w3.org/2001/XMLSchema#boolean

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/object/input.raml#/web-api/end-points/%2Fbad1/user/object_1
  Property: http://a.ml/vocabularies/data#alive
  Position: Some(LexicalInformation([(25,0)-(26,15)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/object/input.raml
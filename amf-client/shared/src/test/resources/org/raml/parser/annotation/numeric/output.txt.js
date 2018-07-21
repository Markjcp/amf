Model: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml
Profile: RAML
Conforms? false
Number of results: 4

Level: Violation

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml#/declarations/annotations/age/scalar/schema_validation_validation_minimum/prop
  Message: Data at / must be greater than or equal to 5
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml#/web-api/end-points/%2FpersonNotOk1/age/scalar_1
  Property: http://a.ml/vocabularies/data#value
  Position: Some(LexicalInformation([(23,9)-(23,10)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml#/declarations/annotations/fingers/scalar/schema_validation_validation_maximum/prop
  Message: Data at / must be smaller than or equal to 10
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml#/web-api/end-points/%2FpersonNotOk2/fingers/scalar_1
  Property: http://a.ml/vocabularies/data#value
  Position: Some(LexicalInformation([(27,13)-(27,15)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml#/declarations/annotations/range/scalar/schema_validation_validation_maximum/prop
  Message: Data at / must be smaller than or equal to 10
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml#/web-api/end-points/%2Frange/range/scalar_1
  Property: http://a.ml/vocabularies/data#value
  Position: Some(LexicalInformation([(29,11)-(29,13)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml#/declarations/annotations/leapYear/scalar/schema_validation_validation_multipleOf/prop
  Message: Data at is not a multipleOf '4'
  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml#/web-api/end-points/%2FyearsBad/leapYear/scalar_1
  Property: http://a.ml/vocabularies/data#value
  Position: Some(LexicalInformation([(33,14)-(33,18)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/annotation/numeric/input.raml
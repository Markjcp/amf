Model: file://amf-client/shared/src/test/resources/org/raml/parser/types/includes/input.raml
Profile: RAML
Conforms? false
Number of results: 1

Level: Violation

- Source: file://amf-client/shared/src/test/resources/org/raml/parser/types/includes/book.raml#/type_validation
  Message: Object at / must be valid
Data at //name must have length smaller than 10
Array items at //chapters must be valid
Data at //chapters/items/name must have length smaller than 10

  Level: Violation
  Target: file://amf-client/shared/src/test/resources/org/raml/parser/types/includes/book.raml#/shape/example/default-example
  Property: file://amf-client/shared/src/test/resources/org/raml/parser/types/includes/book.raml#/shape/example/default-example
  Position: Some(LexicalInformation([(14,0)-(23,16)]))
  Location: file://amf-client/shared/src/test/resources/org/raml/parser/types/includes/input.raml
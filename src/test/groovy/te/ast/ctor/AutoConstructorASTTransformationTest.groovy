package te.ast.ctor

import spock.lang.Specification
import te.ast.ctor.domain.AnnotationSettings

class AutoConstructorASTTransformationTest extends Specification {

    AutoConstructorASTTransformation astTransformation = []

//    def "can correctly parse settings from annotation"() {
//        when:
//            AnnotationSettings settings = astTransformation.readSettingsFromAnnotation()
//
//        then:
//            // TODO implement assertions
//    }

    @AutoConstructor
    class Dummy {}
}

package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION

@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: "there's only one constructor"
        assert annotatedClass.declaredConstructors.size() == 1
        def constructor = annotatedClass.declaredConstructors[0]
        assert constructor

    and: "it only accepts one parameter"
        assert constructor.parameters.size() == 1

    and: 'the constructor has the @Inject annotation on it'
        assert constructor.annotations.size() == 1
        assert constructor.annotations.first().classNode.name == "javax.inject.Inject"
})
@AutoConstructor
class InjectEnabled {
    String someProperty
}

@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: "there's only one constructor"
        assert annotatedClass.declaredConstructors.size() == 1
        def constructor = annotatedClass.declaredConstructors[0]

    and: "it only accepts one parameter"
        assert constructor.parameters.size() == 1

    and: 'it is not annotated with @Inject'
        assert constructor.annotations.isEmpty()
})
@AutoConstructor(addInjectAnnotation = false)
class InjectDisabled {
    String someProperty
}
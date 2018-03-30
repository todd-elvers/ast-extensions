package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: "Only one constructor w/ one argument is added"
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 1

    and: 'the constructor has the @Inject annotation on it'
        assert annotatedClass.declaredConstructors[0].annotations.size() == 1
        assert annotatedClass.declaredConstructors[0].annotations.first().classNode.name == "javax.inject.Inject"
})
@SingleConstructor
class DefaultScenario {
    String someProperty
}
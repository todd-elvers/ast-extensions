package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION

@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with one arg
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 1

    // That arg should be our field without a default value
    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someField"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()
})
@SingleConstructor
class ClassWithAnExistingConstructor {
    String someField
    String someOtherField

    ClassWithAnExistingConstructor(String someField) {
        this.someField = someField
    }
}

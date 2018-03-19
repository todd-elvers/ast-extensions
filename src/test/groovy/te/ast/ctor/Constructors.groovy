package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION

@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have the constructor that was already present
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 1

    // With the param it already had
    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someField"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()
})
@SingleConstructor
class ExistingConstructor {
    String someField
    String someOtherField

    ExistingConstructor(String someField) {
        this.someField = someField
    }
}


@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should have both the existing constructor and the one we added
    assert classNode.declaredConstructors.size() == 2
    assert classNode.declaredConstructors[0].parameters.size() == 1
    assert classNode.declaredConstructors[1].parameters.size() == 2

    // First constructor should be unchanged
    def firstConstructorFirstArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorFirstArg.name == "someField"
    assert firstConstructorFirstArg.type.text == "java.lang.String"
    assert !firstConstructorFirstArg.hasInitialExpression()

    // Second constructor, the one we added, should have 2 params
    def secondConstructorFirstArg = classNode.declaredConstructors[1].parameters[0]
    assert secondConstructorFirstArg.name == "someField"
    assert secondConstructorFirstArg.type.text == "java.lang.String"
    assert !secondConstructorFirstArg.hasInitialExpression()
    def secondConstructorSecondArg = classNode.declaredConstructors[1].parameters[1]
    assert secondConstructorSecondArg.name == "someOtherField"
    assert secondConstructorSecondArg.type.text == "java.lang.String"
    assert !secondConstructorSecondArg.hasInitialExpression()
})
@SingleConstructor(force = true)
class ExistingConstructorWithForce {
    String someField
    String someOtherField

    ExistingConstructorWithForce(String someField) {
        this.someField = someField
    }
}

@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // One constructor should get injected with one arg
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 1

    // The injected constructor should have the @Inject annotation on it
    assert classNode.declaredConstructors[0].annotations.size() == 1
    assert classNode.declaredConstructors[0].annotations.first().classNode.name == "javax.inject.Inject"
})
@SingleConstructor
class InjectAnnotation {
    String someProperty
}
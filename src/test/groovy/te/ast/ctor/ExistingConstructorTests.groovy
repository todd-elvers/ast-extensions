package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode
import spock.lang.Specification
import te.ast.ctor.AutoConstructor

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION

class ExistingConstructorTests extends Specification {

    def "when force = false and an existing constructor is found: do nothing"() {
        given:
            def inputArg = "someString"
            def instance = new ExistingConstructorUsingDefaults(inputArg)

        expect: 'the existing constructor to be unchanged'
            instance.someField == inputArg
            instance.someOtherField == null
    }

    def "when force = true and an existing constructor is found: add our constructor"() {
        given:
            def firstInputArg = "someString"
            def secondInputArg = "someOtherString"

        when:
            def instance = new ExistingConstructorWithForce(firstInputArg)

        then: 'the existing constructor was unchanged'
            instance.someField == firstInputArg
            instance.someOtherField == null

        when: 'we try with both args'
            instance = new ExistingConstructorWithForce(firstInputArg, secondInputArg)

        then: 'our added constructor works as expected'
            instance.someField == firstInputArg
            instance.someOtherField == secondInputArg
    }


}

////////////////////////////////////////////
//////////// Compile time tests ////////////
////////////////////////////////////////////

@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: "we only find the original constructor"
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 1

    and: "it has the original parameter too"
        def firstConstructorArg = annotatedClass.declaredConstructors[0].parameters[0]
        assert firstConstructorArg.name == "someField"
        assert firstConstructorArg.type.text == "java.lang.String"
        assert !firstConstructorArg.hasInitialExpression()
})
@AutoConstructor
class ExistingConstructorUsingDefaults {
    String someField
    String someOtherField

    ExistingConstructorUsingDefaults(String someField) {
        this.someField = someField
    }
}


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'there are now two constructors'
        assert annotatedClass.declaredConstructors.size() == 2
        assert annotatedClass.declaredConstructors[1].parameters.size() == 2

    and: 'the existing one'
        assert annotatedClass.declaredConstructors[0].parameters.size() == 1
        def firstConstructorFirstArg = annotatedClass.declaredConstructors[0].parameters[0]
        assert firstConstructorFirstArg.name == "someField"
        assert firstConstructorFirstArg.type.text == "java.lang.String"
        assert !firstConstructorFirstArg.hasInitialExpression()

    and: 'the one we added'
        def secondConstructorFirstArg = annotatedClass.declaredConstructors[1].parameters[0]
        assert secondConstructorFirstArg.name == "someField"
        assert secondConstructorFirstArg.type.text == "java.lang.String"
        assert !secondConstructorFirstArg.hasInitialExpression()
        def secondConstructorSecondArg = annotatedClass.declaredConstructors[1].parameters[1]
        assert secondConstructorSecondArg.name == "someOtherField"
        assert secondConstructorSecondArg.type.text == "java.lang.String"
        assert !secondConstructorSecondArg.hasInitialExpression()
})
@AutoConstructor(force = true)
class ExistingConstructorWithForce {
    String someField
    String someOtherField

    ExistingConstructorWithForce(String someField) {
        this.someField = someField
    }
}





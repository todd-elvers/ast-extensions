package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode
import spock.lang.Specification

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION

class PropertiesTests extends Specification {

    String arg1 = "someString"
    List<String> secondArg = ["string", "string", "string"]

    def "generated constructor correctly assigns properties"() {
        when:
            def instance = new SingleProperty(arg1)

        then:
            instance.someProperty == arg1

        when:
            instance = new MultipleProperties(arg1, secondArg)
        then:
            instance.someProperty == arg1
            instance.someListProperty == secondArg
    }

    def "setting 'excludes' parameter works"() {
        when:
            def instance = new PropertiesWithExcludes(arg1)

        then:
            instance.someProperty == arg1
    }

    def "setting 'include' parameter works"() {
        when:
            def instance = new PropertiesWithIncludes(arg1)
        then:
            instance.someProperty == arg1
    }


    def 'setting includeFields=true & includeProperties=false works'() {
        when:
            def instance = new IgnoringPropertiesIncludingFields(arg1)
        then:
            instance.someProperty == arg1
    }

    def 'setting includeFields=false & includeProperties=true works'() {
        when:
            def instance = new PropertiesIgnoringFields(arg1)
        then:
            instance.someProperty == arg1
    }

    def 'setting includeFields=true & includeProperties=true works'() {
        given:
            String firstArg = "someString"
            String secondArg = "someOtherString"
            boolean thirdArg = true
        when:
            def instance = new PropertiesIncludingFields(firstArg, secondArg, thirdArg)
        then:
            instance.someProperty == firstArg
            instance.someField1 == secondArg
            instance.someField2 == thirdArg
    }

}


////////////////////////////////////////////
//////////// Compile time tests ////////////
////////////////////////////////////////////

@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find 1 constructor with 0 args'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 0
        assert annotatedClass.declaredConstructors[0].name == "<init>"
        assert !annotatedClass.declaredConstructors[0].parameters
})
@AutoConstructor
class ZeroProperties {
}


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find 1 constructor with 1 arg'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 1

    and: 'that arg is our property, without a default value'
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == "someProperty"
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()
})
@AutoConstructor
class SingleProperty {
    String someProperty
}


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find 1 constructor with 2 args'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 2

    and: 'those args are ordered how they are listed in the class from top to bottom'
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == "someProperty"
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()

        def arg2 = annotatedClass.declaredConstructors[0].parameters[1]
        assert arg2.name == "someListProperty"
        assert arg2.type.text == "java.util.List"
        assert !arg2.hasInitialExpression()
})
@AutoConstructor
class MultipleProperties {
    String someProperty
    List<String> someListProperty
}

@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find 1 constructor with 1 arg'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 1

    and: "that arg is the property/field we did not specify in the 'excludes' parameter"
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == "someProperty"
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()
})
@AutoConstructor(excludes = "someListProperty")
class PropertiesWithExcludes {
    List<String> someListProperty
    String someProperty
}


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find 1 constructor with 1 arg'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 1

    and: "that arg is the property/field we specified in the 'includes' parameter"
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == "someProperty"
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()
})
@AutoConstructor(includes = "someProperty")
class PropertiesWithIncludes {
    List<String> someListProperty
    String someProperty
}

@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find only 1 constructor with 1 arg'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 1

    and: 'that arg is for the field of our class, not the property'
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == "someProperty"
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()
})
@AutoConstructor(includeFields = true, includeProperties = false, excludes = 'metaClass')
class IgnoringPropertiesIncludingFields {
    private String someProperty
    List<String> someListProperty
}


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find one constructor with 1 arg'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 1

    and: 'that arg is our property and does not have a default value'
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == "someProperty"
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()
})
@AutoConstructor
class PropertiesIgnoringFields {
    String someProperty
    private String someField1
    protected boolean someField2
}


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find 1 constructor with 3 args'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 3

    and: 'the args are in the same order as they are listed in the class'
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == "someProperty"
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()

        def arg2 = annotatedClass.declaredConstructors[0].parameters[1]
        assert arg2.name == "someField1"
        assert arg2.type.text == "java.lang.String"
        assert !arg2.hasInitialExpression()

        def arg3 = annotatedClass.declaredConstructors[0].parameters[2]
        assert arg3.name == "someField2"
        assert arg3.type.text == "boolean"
        assert !arg3.hasInitialExpression()
})
@AutoConstructor(includeFields = true, excludes = "metaClass")
class PropertiesIncludingFields {
    String someProperty
    private String someField1
    protected boolean someField2
}


package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode
import org.spockframework.compiler.model.Spec
import spock.lang.Specification

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION

class SuperTests extends Specification {

    String arg1 = "arg1"
    String arg2 = "arg2"

    def "setting includeSuperFields=true works"() {
        when:
          def instance = new SuperWithFields(arg1, arg2)

        then:
            instance.someSuperField == arg1
            instance.someProperty == arg2
    }

    def "setting includeSuperProperties=true works"() {
        when:
            def instance = new SuperWithProperties(arg1, arg2)

        then:
            instance.someSuperProperty == arg1
            instance.someProperty == arg2
    }

    def  "setting includeSuperProperties=true & includeSuperFields=true works"() {
        given:
            def arg3 = "arg3"
        when:
            def instance = new SuperWithPropertiesAndFields(arg1, arg2, arg3)
        then:
            instance.someSuperProperty == arg1
            instance.someSuperField == arg2
            instance.someProperty == arg3
    }

    def "setting includeSuperProperties=true & callSuper=true works"() {
        given:
            String arg1 = "arg1"
            String arg2 = "arg2"

        when:
            def instance = new SuperWithPropertyAndConstructor(arg1, arg2)

        then:
            instance.someSuperProperty == "${arg1}_from_super"
            instance.someProperty == arg2
    }
}


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find 1 constructor with 2 args'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 2

    and: "the first arg is for the super's field"
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == 'someSuperField'
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()

    and: "the second arg is for the annotated class's property"
        def arg2 = annotatedClass.declaredConstructors[0].parameters[1]
        assert arg2.name == "someProperty"
        assert arg2.type.text == "java.lang.String"
        assert !arg2.hasInitialExpression()
})
@AutoConstructor(includeSuperFields = true, excludes = 'metaClass')
class SuperWithFields extends Fields {
    String someProperty
}

abstract class Fields {
    protected String someSuperField
}

@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: 'we find 1 constructor with 2 args'
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 2

    and: "the first arg is for the super's property"
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == 'someSuperProperty'
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()

    and: "the second arg is for the annotated class's property"
        def arg2 = annotatedClass.declaredConstructors[0].parameters[1]
        assert arg2.name == "someProperty"
        assert arg2.type.text == "java.lang.String"
        assert !arg2.hasInitialExpression()
})
@AutoConstructor(includeSuperProperties = true)
class SuperWithProperties extends Properties {
    String someProperty
}

abstract class Properties {
    String someSuperProperty
}

@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: "we find 1 constructor with 3 args"
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 3

    and: "the first arg is for the super's property"
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == 'someSuperProperty'
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()

    and: "the second arg is for the super's field"
        def arg2 = annotatedClass.declaredConstructors[0].parameters[1]
        assert arg2.name == "someSuperField"
        assert arg2.type.text == "java.lang.String"
        assert !arg2.hasInitialExpression()

    and: "the third arg is for the annotated class's property"
        def arg3 = annotatedClass.declaredConstructors[0].parameters[2]
        assert arg3.name == "someProperty"
        assert arg3.type.text == "java.lang.String"
        assert !arg3.hasInitialExpression()
})
@AutoConstructor(includeSuperProperties = true, includeSuperFields = true, excludes = 'metaClass')
class SuperWithPropertiesAndFields extends PropertiesAndFields {
    String someProperty
}

abstract class PropertiesAndFields {
    String someSuperProperty
    protected String someSuperField
}


@ASTTest(phase = CANONICALIZATION, value = {
    when: 'we inspect the result of applying our AST to the class below'
        def annotatedClass = node as ClassNode

    then: "we find 1 constructor with 2 args"
        assert annotatedClass.declaredConstructors.size() == 1
        assert annotatedClass.declaredConstructors[0].parameters.size() == 2

    and: "first is for the super's property"
        def arg1 = annotatedClass.declaredConstructors[0].parameters[0]
        assert arg1.name == "someSuperProperty"
        assert arg1.type.text == "java.lang.String"
        assert !arg1.hasInitialExpression()

    and: "the second is for the annotated class's property"
        def arg2 = annotatedClass.declaredConstructors[0].parameters[1]
        assert arg2.name == "someProperty"
        assert arg2.type.text == "java.lang.String"
        assert !arg2.hasInitialExpression()
})
@AutoConstructor(includeSuperProperties = true, callSuper = true)
class SuperWithPropertyAndConstructor extends PropertyWithConstructor {
    String someProperty
}

abstract class PropertyWithConstructor {
    String someSuperProperty

    PropertyWithConstructor() {}

    PropertyWithConstructor(String someSuperProperty) {
        this.someSuperProperty = someSuperProperty + "_from_super"
    }
}
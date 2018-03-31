package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION


@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with one arg
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 2

    // That arg should be our property without a default value
    def firstArg = classNode.declaredConstructors[0].parameters[0]
    assert firstArg.name == 'someSuperProperty'
    assert firstArg.type.text == "java.lang.String"
    assert !firstArg.hasInitialExpression()

    def thirdArg = classNode.declaredConstructors[0].parameters[1]
    assert thirdArg.name == "someProperty"
    assert thirdArg.type.text == "java.lang.String"
    assert !thirdArg.hasInitialExpression()
})
@AutoConstructor(includeSuperProperties = true)
class SuperWithProperties extends Properties {
    String someProperty
}
abstract class Properties {
    String someSuperProperty
    protected String someSuperField
}

@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with one arg
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 3

    // That arg should be our property without a default value
    def firstArg = classNode.declaredConstructors[0].parameters[0]
    assert firstArg.name == 'someSuperProperty'
    assert firstArg.type.text == "java.lang.String"
    assert !firstArg.hasInitialExpression()

    def thirdArg = classNode.declaredConstructors[0].parameters[1]
    assert thirdArg.name == "someSuperField"
    assert thirdArg.type.text == "java.lang.String"
    assert !thirdArg.hasInitialExpression()

    def fourthArg = classNode.declaredConstructors[0].parameters[2]
    assert fourthArg.name == "someProperty"
    assert fourthArg.type.text == "java.lang.String"
    assert !fourthArg.hasInitialExpression()
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
    def classNode = node as ClassNode

    // Should only have one constructor with one arg
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 2

    // That arg should be our property without a default value
    def firstArg = classNode.declaredConstructors[0].parameters[0]
    assert firstArg.name == "someSuperProperty"
    assert firstArg.type.text == "java.lang.String"
    assert !firstArg.hasInitialExpression()

    def secondArg = classNode.declaredConstructors[0].parameters[1]
    assert secondArg.name == "someProperty"
    assert secondArg.type.text == "java.lang.String"
    assert !secondArg.hasInitialExpression()
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

//TODO: Separate out ASTTests & unit tests
//def result = new SuperWithPropertyAndConstructor("arg1", "arg2")
//assert result.someSuperProperty == "arg1_from_super"
//assert result.someProperty == "arg2"
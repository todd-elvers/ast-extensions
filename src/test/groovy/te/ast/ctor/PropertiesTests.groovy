package te.ast.ctor

import groovy.transform.ASTTest
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.Parameter
import te.ast.ctor.SingleConstructor

import static org.codehaus.groovy.control.CompilePhase.CANONICALIZATION

@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with no args
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 0

    // That constructor should be the no-arg constructor
    def constructor = classNode.declaredConstructors[0]
    assert constructor.name == "<init>"
    assert !constructor.parameters
})
@SingleConstructor
class ZeroProperties {
}



@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with one arg
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 1

    // That arg should be our property without a default value
    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someProperty"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()
})
@SingleConstructor
class OneProperty {
    String someProperty
}



@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with two args
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 2

    // First arg should be our first property without a default value
    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someProperty"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()

    // Second arg should be our second property without a default value
    def secondConstructorArg = classNode.declaredConstructors[0].parameters[1]
    assert secondConstructorArg.name == "someListProperty"
    assert secondConstructorArg.type.text == "java.util.List"
    assert !secondConstructorArg.hasInitialExpression()
})
@SingleConstructor
class TwoProperties {
    String someProperty
    List<String> someListProperty
}

@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with one args
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 1

    // Only arg should be the property we didn't specify in the 'excludes' list
    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someProperty"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()
})
@SingleConstructor(excludes = "someListProperty")
class PropertiesWithExcludes {
    String someProperty
    List<String> someListProperty
}


@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with one args
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 1

    // Only arg should be the property we specified in the 'includes' list
    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someProperty"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()
})
@SingleConstructor(includes = "someProperty")
class PropertiesWithIncludes {
    String someProperty
    List<String> someListProperty
}

@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with one args
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 1

    // Only arg should be the only field of the class, and no properties
    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someProperty"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()
})
@SingleConstructor(includeFields = true, includeProperties = false, excludes = 'metaClass')
class IgnoringPropertiesIncludingFields {
    private String someProperty
    List<String> someListProperty
}


@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with one arg
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 1

    // That arg should be our property without a default value
    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someProperty"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()
})
@SingleConstructor
class PropertiesIgnoringFields {
    String someProperty
    private String someField1
    protected boolean someField2
}


@ASTTest(phase = CANONICALIZATION, value = {
    def classNode = node as ClassNode

    // Should only have one constructor with 3 args, excluding metaClass
    assert classNode.declaredConstructors.size() == 1
    assert classNode.declaredConstructors[0].parameters.size() == 3

    def firstConstructorArg = classNode.declaredConstructors[0].parameters[0]
    assert firstConstructorArg.name == "someProperty"
    assert firstConstructorArg.type.text == "java.lang.String"
    assert !firstConstructorArg.hasInitialExpression()

    def secondConstructorArg = classNode.declaredConstructors[0].parameters[1]
    assert secondConstructorArg.name == "someField1"
    assert secondConstructorArg.type.text == "java.lang.String"
    assert !secondConstructorArg.hasInitialExpression()

    def thirdConstructorArg = classNode.declaredConstructors[0].parameters[2]
    assert thirdConstructorArg.name == "someField2"
    assert thirdConstructorArg.type.text == "boolean"
    assert !thirdConstructorArg.hasInitialExpression()
})
@SingleConstructor(includeFields = true, excludes = "metaClass")
class PropertiesIncludingFields {
    String someProperty
    private String someField1
    protected boolean someField2
}


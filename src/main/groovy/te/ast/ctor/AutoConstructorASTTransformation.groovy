package te.ast.ctor

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.VariableScope
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TupleConstructorASTTransformation

import javax.inject.Inject

import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.ClassHelper.makeWithoutCaching
import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.block
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorSuperS
import static org.codehaus.groovy.ast.tools.GeneralUtils.getInstanceNonPropertyFields
import static org.codehaus.groovy.ast.tools.GeneralUtils.getInstancePropertyFields
import static org.codehaus.groovy.ast.tools.GeneralUtils.getSuperNonPropertyFields
import static org.codehaus.groovy.ast.tools.GeneralUtils.getSuperPropertyFields
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class AutoConstructorASTTransformation extends AbstractASTTransformation {

    private static final ClassNode MY_TYPE = make(AutoConstructor)
    private static final ClassNode HASH_MAP_TYPE = makeWithoutCaching(HashMap, false)
    private static final AnnotationNode INJECT_ANNOTATION = new AnnotationNode(make(Inject))
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage()

    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)

        AnnotationNode annotation = nodes[0] as AnnotationNode
        AnnotatedNode annotatedClass = nodes[1] as AnnotatedNode

        if (annotatedClass instanceof ClassNode) {
            visitClass(annotatedClass, annotation)
        }
    }

    protected void visitClass(ClassNode annotatedClass, AnnotationNode annotation) {
        AutoConstructorSettings settings = AutoConstructorParser.parse(annotation)

        if (isValidUseOfAST(annotation, annotatedClass, settings)) {
            ConstructorNode constructor = generateConstructor(annotatedClass, settings)

            if(settings.addInjectAnnotation) {
                constructor.addAnnotation(INJECT_ANNOTATION)
            }

            addConstructor(annotatedClass, constructor)
        }
    }

    protected boolean isValidUseOfAST(AnnotationNode anno, ClassNode annotatedClass, AutoConstructorSettings settings) {
        // Annotation not allowed on interfaces
        if(!checkNotInterface(annotatedClass, MY_TYPE_NAME)) return false

        // Cannot proceed if 'includes' & 'excludes' are improperly configured
        if(!checkIncludeExclude(anno, settings.excludes, settings.includes, MY_TYPE_NAME)) return false

        // Cannot proceed if there's more than 1 constructor, unless force=true
        List<ConstructorNode> constructors = annotatedClass.declaredConstructors
        if(constructors.size() > 1 && !settings.force) return false

        // Cannot proceed if there's 1 constructor, unless it's the empty constructor or force=true
        boolean foundEmptyConstructor = constructors.size() == 1 && !constructors.first().firstStatement
        if(constructors.size() == 1 && !foundEmptyConstructor && !settings.force) return false

        // If we did find the empty constructor, remove it (JavaStubGenerator may have snuck one in)
        if (foundEmptyConstructor) constructors.remove(0)

        return true
    }

    protected void addConstructor(ClassNode annotatedClass, ConstructorNode constructor) {
        annotatedClass.addConstructor(constructor)

        if (constructor.parameters) {
            handleMapConstructorInjection(annotatedClass, constructor.parameters)
        }
    }

    /**
     * Parses annotated class creating a constructor according to the settings configured
     * in the {@link AutoConstructor} annotation.
     *
     * <p>The resulting constructor has 0-n args and a corresponding number of "this.<field> = <arg>"
     * statements as its body.
     */
    protected ConstructorNode generateConstructor(ClassNode annotatedClass, AutoConstructorSettings settings) {
        List<Parameter> ctorParams = []
        List<Statement> ctorStatements = []
        List<Expression> superCtorExpressions = []

        // Parse fields from super
        List<FieldNode> superFieldNodes = getSuperFields(annotatedClass, settings)
        superFieldNodes.each { FieldNode fNode ->
            if (shouldSkip(fNode.name, settings.excludes, settings.includes)) return

            String name = fNode.name
            ctorParams << new Parameter(fNode.type, name)
            if (settings.callSuper) {
                superCtorExpressions << varX(name)
            } else {
                ctorStatements << assignS(propX(varX("this"), name), varX(name))
            }
        }

        // Add a call to the super's constructor if necessary
        if (settings.callSuper) {
            ctorStatements << ctorSuperS(args(superCtorExpressions))
        }

        // Parse fields from instance
        List<FieldNode> instanceFieldNodes = getClassFields(annotatedClass, settings)
        instanceFieldNodes.each { FieldNode fNode ->
            if (shouldSkip(fNode.name, settings.excludes, settings.includes)) return

            String name = fNode.name
            Parameter nextParam = new Parameter(fNode.type, name)
            ctorParams << nextParam
            ctorStatements << assignS(propX(varX("this"), name), varX(nextParam))
        }

        Parameter[] argsOfCtor = ctorParams.toArray(new Parameter[ctorParams.size()]) as Parameter[]
        BlockStatement bodyOfCtor = block(new VariableScope(), ctorStatements)

        return new ConstructorNode(ACC_PUBLIC, argsOfCtor, ClassNode.EMPTY_ARRAY, bodyOfCtor)
    }

    protected List<FieldNode> getSuperFields(ClassNode annotatedClass, AutoConstructorSettings settings) {
        List<FieldNode> superFields = []
        if (settings.includeSuperProperties) {
            superFields += getSuperPropertyFields(annotatedClass.getSuperClass())
        }
        if (settings.includeSuperFields) {
            superFields += getSuperNonPropertyFields(annotatedClass.getSuperClass())
        }
        return superFields
    }

    protected List<FieldNode> getClassFields(ClassNode annotatedClass, AutoConstructorSettings settings) {
        List<FieldNode> fields = []
        if (settings.includeProperties) {
            fields += getInstancePropertyFields(annotatedClass)
        }
        if (settings.includeFields) {
            fields += getInstanceNonPropertyFields(annotatedClass)
        }
        return fields
    }

    /**
     * Adds a map constructor to a class if the first constructor {@link Parameter}
     * is of type {@link Map} or some superclass of {@link HashMap} (excluding LinkedHashMap).
     */
    protected void handleMapConstructorInjection(ClassNode annotatedClass, Parameter[] constructorArgs) {
        ClassNode firstParam = constructorArgs.first().getType()

        // add map constructor if needed, don't do it for LinkedHashMap for now (would lead to duplicate signature)
        // or if there is only one Map property (for backwards compatibility)
        if (constructorArgs.size() > 1 || firstParam == ClassHelper.OBJECT_TYPE) {
            if (firstParam == ClassHelper.MAP_TYPE) {
                addMapConstructors(annotatedClass)
            } else {
                ClassNode candidate = HASH_MAP_TYPE
                while (candidate) {
                    if (candidate == firstParam) {
                        addMapConstructors(annotatedClass)
                        break
                    }

                    candidate = candidate.getSuperClass()
                }
            }
        }
    }

    /**
     * To reduce code duplication we simply delegate to {@link TupleConstructorASTTransformation}
     * since it already does what we need here.
     */
    protected void addMapConstructors(ClassNode annotatedClass) {
        TupleConstructorASTTransformation.addMapConstructors(
                annotatedClass,
                true,
                "The class ${annotatedClass.name} was incorrectly initialized via the map constructor with null."
        )
    }
}

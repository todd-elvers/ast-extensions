package te.ast.ctor

import groovy.transform.Canonical
import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.FieldNode
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.codehaus.groovy.transform.TupleConstructorASTTransformation
import te.ast.ctor.domain.AnnotationSettings

import javax.inject.Inject

import static org.codehaus.groovy.ast.ClassHelper.make
import static org.codehaus.groovy.ast.ClassHelper.makeWithoutCaching
import static org.codehaus.groovy.ast.tools.GeneralUtils.args
import static org.codehaus.groovy.ast.tools.GeneralUtils.assignS
import static org.codehaus.groovy.ast.tools.GeneralUtils.ctorX
import static org.codehaus.groovy.ast.tools.GeneralUtils.getInstanceNonPropertyFields
import static org.codehaus.groovy.ast.tools.GeneralUtils.getInstancePropertyFields
import static org.codehaus.groovy.ast.tools.GeneralUtils.getSuperNonPropertyFields
import static org.codehaus.groovy.ast.tools.GeneralUtils.getSuperPropertyFields
import static org.codehaus.groovy.ast.tools.GeneralUtils.propX
import static org.codehaus.groovy.ast.tools.GeneralUtils.stmt
import static org.codehaus.groovy.ast.tools.GeneralUtils.varX

@CompileStatic
@SuppressWarnings("GrMethodMayBeStatic")
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class SingleConstructorASTTransformation extends AbstractASTTransformation {

    private static final ClassNode MY_TYPE = make(SingleConstructor)
    private static final ClassNode CANONICAL_ANNOTATION_CLASS = make(Canonical)
    private static final ClassNode HASHMAP_TYPE = makeWithoutCaching(HashMap, false)
    private static final AnnotationNode INJECT_ANNOTATION = new AnnotationNode(make(Inject))
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage()

    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)

        AnnotationNode annotation = nodes[0] as AnnotationNode
        AnnotatedNode annotatedClass = nodes[1] as AnnotatedNode

        if (wasTriggeredByWrongAnnotation(annotation)) return

        if (annotatedClass instanceof ClassNode) {
            visitClass(annotatedClass, annotation)
        }
    }

    protected boolean wasTriggeredByWrongAnnotation(AnnotationNode annotation) {
        return annotation.classNode != MY_TYPE
    }

    protected void visitClass(ClassNode annotatedClass, AnnotationNode annotation) {
        AnnotationSettings settings = readSettingsFromAnnotation(annotation)
        if (hasAnnotation(annotatedClass, CANONICAL_ANNOTATION_CLASS)) {
            copySettingsFromCanonicalAST(annotatedClass, settings)
        }

        if (isValidUseOfAST(annotation, annotatedClass, settings)) {
            injectConstructor(annotatedClass, settings)
        }
    }

    protected boolean isValidUseOfAST(AnnotationNode anno, ClassNode annotatedClass, AnnotationSettings astParams) {
        // Annotation not allowed on interfaces
        if(!checkNotInterface(annotatedClass, MY_TYPE_NAME)) return false

        // Cannot proceed if 'includes' & 'excludes' are improperly configured
        if(!checkIncludeExclude(anno, astParams.excludes, astParams.includes, MY_TYPE_NAME)) return false

        // Don't do anything if constructors exist and force is set to false
        List<ConstructorNode> constructors = annotatedClass.declaredConstructors
        if(constructors.size() > 1 && !astParams.force) return false
        boolean foundEmpty = constructors.size() == 1 && !constructors.first().firstStatement
        if(constructors.size() == 1 && !foundEmpty && !astParams.force) return false

        // HACK: JavaStubGenerator could have snuck in a constructor we don't want
        if (foundEmpty) constructors.remove(0)

        return true
    }

    protected AnnotationSettings readSettingsFromAnnotation(AnnotationNode anno) {
        return [
                includeFields         : memberHasValue(anno, "includeFields", true),
                includeProperties     : !memberHasValue(anno, "includeProperties", false),
                includeSuperFields    : memberHasValue(anno, "includeSuperFields", true),
                includeSuperProperties: memberHasValue(anno, "includeSuperProperties", true),
                callSuper             : memberHasValue(anno, "callSuper", true),
                force                 : memberHasValue(anno, "force", true),
                excludes              : getMemberList(anno, "excludes"),
                includes              : getMemberList(anno, "includes")
        ] as AnnotationSettings
    }

    protected void copySettingsFromCanonicalAST(ClassNode annotatedClass, AnnotationSettings settings) {
        AnnotationNode canonical = annotatedClass.getAnnotations(CANONICAL_ANNOTATION_CLASS).first()
        if (!settings.excludes) {
            settings.excludes = getMemberList(canonical, "excludes")
        }
        if (!settings.includes) {
            settings.includes = getMemberList(canonical, "includes")
        }
    }

    protected static void injectConstructor(ClassNode annotatedClass, AnnotationSettings settings) {
        ConstructorNode constructor = buildConstructor(annotatedClass, settings)

        annotatedClass.addConstructor(constructor)

        if (constructor.parameters) {
            handleMapConstructorInjection(annotatedClass, constructor.parameters)
        }
    }

    protected static ConstructorNode buildConstructor(ClassNode annotatedClass, AnnotationSettings settings) {
        BlockStatement constructorBody = new BlockStatement()
        List<Parameter> constructorArgs = []

        // Build constructorArgs from the super's fields
        List<Expression> superParams = []
        List<FieldNode> superFieldNodes = collectSuperFieldNodes(annotatedClass, settings)
        superFieldNodes.each { FieldNode fNode ->
            if (shouldSkip(fNode.name, settings.excludes, settings.includes)) return

            String name = fNode.name
            constructorArgs << new Parameter(fNode.type, name)
            if (settings.callSuper) {
                superParams << varX(name)
            } else {
                constructorBody.addStatement(assignS(propX(varX("this"), name), varX(name)))
            }
        }

        // Add a call to the super's constructor if necessary
        if (settings.callSuper) {
            constructorBody.addStatement(stmt(ctorX(ClassNode.SUPER, args(superParams))))
        }

        // Build constructorArgs from the instance's fields
        List<FieldNode> instanceFieldNodes = collectInstanceFieldNodes(annotatedClass, settings)
        instanceFieldNodes.each { FieldNode fNode ->
            if (shouldSkip(fNode.name, settings.excludes, settings.includes)) return

            String name = fNode.name
            Parameter nextParam = new Parameter(fNode.type, name)
            constructorArgs << nextParam
            constructorBody.addStatement(assignS(propX(varX("this"), name), varX(nextParam)))
        }

        return createConstructor(constructorArgs, constructorBody)
    }

    protected static ConstructorNode createConstructor(List<Parameter> args, BlockStatement body) {
        def constructor = new ConstructorNode(
                ACC_PUBLIC,
                args.toArray(new Parameter[args.size()]),
                ClassNode.EMPTY_ARRAY,
                body
        )
        constructor.addAnnotation(INJECT_ANNOTATION)
        return constructor
    }

    protected static List<FieldNode> collectSuperFieldNodes(ClassNode annotatedClass, AnnotationSettings settings) {
        List<FieldNode> superFields = []
        if (settings.includeSuperProperties) {
            superFields += getSuperPropertyFields(annotatedClass.getSuperClass())
        }
        if (settings.includeSuperFields) {
            superFields += getSuperNonPropertyFields(annotatedClass.getSuperClass())
        }
        return superFields
    }

    protected static List<FieldNode> collectInstanceFieldNodes(ClassNode annotatedClass, AnnotationSettings settings) {
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
    private static void handleMapConstructorInjection(ClassNode annotatedClass, Parameter[] constructorArgs) {
        ClassNode firstParam = constructorArgs.first().getType()

        // add map constructor if needed, don't do it for LinkedHashMap for now (would lead to duplicate signature)
        // or if there is only one Map property (for backwards compatibility)
        if (constructorArgs.size() > 1 || firstParam == ClassHelper.OBJECT_TYPE) {
            if (firstParam == ClassHelper.MAP_TYPE) {
                addMapConstructors(annotatedClass)
            } else {
                ClassNode candidate = HASHMAP_TYPE
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
    private static void addMapConstructors(ClassNode annotatedClass) {
        TupleConstructorASTTransformation.addMapConstructors(
                annotatedClass,
                true,
                "The class ${annotatedClass.name} was incorrectly initialized via the map constructor with null."
        )
    }
}

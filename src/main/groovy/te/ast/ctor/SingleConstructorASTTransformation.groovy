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
import te.ast.ctor.domain.ASTParams
import te.ast.ctor.domain.ParsedAST

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

    public static final Class MY_CLASS = SingleConstructor
    public static final ClassNode MY_TYPE = make(MY_CLASS)
    public static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage()
    private static final ClassNode CANONICAL_ANNOTATION_CLASS = make(Canonical)
    private static final AnnotationNode INJECT_ANNOTATION = new AnnotationNode(make(Inject))
    private static final ClassNode HMAP_TYPE = makeWithoutCaching(HashMap, false)

    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)

        AnnotatedNode parent = nodes[1] as AnnotatedNode
        AnnotationNode anno = nodes[0] as AnnotationNode

        if (anno.classNode != MY_TYPE) {
            return
        }

        if (parent instanceof ClassNode) {
            visitClass(parent, anno)
        }
    }

    protected void visitClass(ClassNode node, AnnotationNode annotation) {
        ASTParams astParams = readASTParamsFromAnnotation(annotation)
        if (hasAnnotation(node, CANONICAL_ANNOTATION_CLASS)) {
            copyParamsFromCanonicalAST(node, astParams)
        }

        if (isValidUseOfAST(annotation, node, astParams)) {
            injectConstructor(node, astParams)
        }
    }

    protected boolean isValidUseOfAST(AnnotationNode anno, ClassNode cNode, ASTParams astParams) {
        // Annotation not allowed on interfaces
        if(!checkNotInterface(cNode, MY_TYPE_NAME)) return false

        // Cannot proceed if 'includes' & 'excludes' are improperly configured
        if(!checkIncludeExclude(anno, astParams.excludes, astParams.includes, MY_TYPE_NAME)) return false

        // Don't do anything if constructors exist and force is set to false
        List<ConstructorNode> ctors = cNode.declaredConstructors
        if(ctors.size() > 1 && !astParams.force) return false
        boolean foundEmpty = ctors.size() == 1 && !ctors.first().firstStatement
        if(ctors.size() == 1 && !foundEmpty && !astParams.force) return false

        // HACK: JavaStubGenerator could have snuck in a constructor we don't want
        if (foundEmpty) ctors.remove(0)

        return true
    }

    protected ASTParams readASTParamsFromAnnotation(AnnotationNode anno) {
        return [
                includeFields         : memberHasValue(anno, "includeFields", true),
                includeProperties     : !memberHasValue(anno, "includeProperties", false),
                includeSuperFields    : memberHasValue(anno, "includeSuperFields", true),
                includeSuperProperties: memberHasValue(anno, "includeSuperProperties", true),
                callSuper             : memberHasValue(anno, "callSuper", true),
                force                 : memberHasValue(anno, "force", true),
                excludes              : getMemberList(anno, "excludes"),
                includes              : getMemberList(anno, "includes")
        ] as ASTParams
    }

    protected void copyParamsFromCanonicalAST(ClassNode cNode, ASTParams astParams) {
        AnnotationNode canonical = cNode.getAnnotations(CANONICAL_ANNOTATION_CLASS).first()
        if (!astParams.excludes) {
            astParams.excludes = getMemberList(canonical, "excludes")
        }
        if (!astParams.includes) {
            astParams.includes = getMemberList(canonical, "includes")
        }
    }

    protected static void injectConstructor(ClassNode cNode, ASTParams astParams) {
        ParsedAST parsedAST = parseClassAST(cNode, astParams)

        cNode.addConstructor(
                createConstructor(parsedAST)
        )

        if (parsedAST.constructorArgs) {
            handleMapConstructorInjection(cNode, parsedAST.constructorArgs)
        }
    }

    protected static ConstructorNode createConstructor(ParsedAST parsedAST) {
        def constructor = new ConstructorNode(
                ACC_PUBLIC,
                parsedAST.toParamArray(),
                ClassNode.EMPTY_ARRAY,
                parsedAST.constructorBody
        )
        constructor.addAnnotation(INJECT_ANNOTATION)
        return constructor
    }

    protected static ParsedAST parseClassAST(ClassNode cNode, ASTParams astParams) {
        List<Parameter> params = []
        List<Expression> superParams = []
        BlockStatement body = new BlockStatement()

        // Build params from the super's fields
        List<FieldNode> superFieldNodes = collectSuperFieldNodes(cNode, astParams)
        superFieldNodes.each { FieldNode fNode ->
            if (shouldSkip(fNode.getName(), astParams.excludes, astParams.includes)) return

            String name = fNode.getName()
            params << new Parameter(fNode.getType(), name)
            if (astParams.callSuper) {
                superParams << varX(name)
            } else {
                body.addStatement(assignS(propX(varX("this"), name), varX(name)))
            }
        }

        // Add a call to the super's constructor if necessary
        if (astParams.callSuper) {
            body.addStatement(stmt(ctorX(ClassNode.SUPER, args(superParams))))
        }

        // Build params from the instance's fields
        List<FieldNode> instanceFieldNodes = collectInstanceFieldNodes(cNode, astParams)
        instanceFieldNodes.each { FieldNode fNode ->
            if (shouldSkip(fNode.getName(), astParams.excludes, astParams.includes)) return

            String name = fNode.getName()
            Parameter nextParam = new Parameter(fNode.getType(), name)
            params << nextParam
            body.addStatement(assignS(propX(varX("this"), name), varX(nextParam)))
        }

        return new ParsedAST(constructorArgs: params, constructorBody: body)
    }

    protected static List<FieldNode> collectSuperFieldNodes(ClassNode cNode, ASTParams astParams) {
        List<FieldNode> superList = []
        if (astParams.includeSuperProperties) {
            superList += getSuperPropertyFields(cNode.getSuperClass())
        }
        if (astParams.includeSuperFields) {
            superList += getSuperNonPropertyFields(cNode.getSuperClass())
        }
        return superList
    }

    protected static List<FieldNode> collectInstanceFieldNodes(ClassNode cNode, ASTParams astParams) {
        List<FieldNode> list = []
        if (astParams.includeProperties) {
            list += getInstancePropertyFields(cNode)
        }
        if (astParams.includeFields) {
            list += getInstanceNonPropertyFields(cNode)
        }
        return list
    }

    /**
     * Adds a map constructor to a class if the first constructor {@link Parameter}
     * is of type {@link Map} or some superclass of {@link HashMap} (excluding LinkedHashMap).
     */
    private static void handleMapConstructorInjection(ClassNode cNode, List<Parameter> constructorArgs) {
        ClassNode firstParam = constructorArgs.first().getType()

        // add map constructor if needed, don't do it for LinkedHashMap for now (would lead to duplicate signature)
        // or if there is only one Map property (for backwards compatibility)
        if (constructorArgs.size() > 1 || firstParam == ClassHelper.OBJECT_TYPE) {
            if (firstParam == ClassHelper.MAP_TYPE) {
                addMapConstructors(cNode)
            } else {
                ClassNode candidate = HMAP_TYPE
                while (candidate) {
                    if (candidate == firstParam) {
                        addMapConstructors(cNode)
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
    private static void addMapConstructors(ClassNode cNode) {
        TupleConstructorASTTransformation.addMapConstructors(
                cNode,
                true,
                "The class " + cNode.getName() + " was incorrectly initialized via the map constructor with null."
        )
    }
}

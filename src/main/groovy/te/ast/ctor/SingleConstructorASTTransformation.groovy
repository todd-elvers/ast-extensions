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
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
class SingleConstructorASTTransformation extends AbstractASTTransformation {

    static final Class MY_CLASS = SingleConstructor
    static final ClassNode MY_TYPE = make(MY_CLASS)
    static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage()
    private static final ClassNode CANONICAL_ANNOTATION = make(Canonical)
    private static final ClassNode HMAP_TYPE = makeWithoutCaching(HashMap, false)

    void visit(ASTNode[] nodes, SourceUnit source) {
        init(nodes, source)

        AnnotatedNode parent = nodes[1] as AnnotatedNode
        AnnotationNode anno = nodes[0] as AnnotationNode

        if (MY_TYPE != anno.getClassNode()) {
            return
        }

        if (parent instanceof ClassNode) {
            visitClass(parent, anno)
        }
    }

    void visitClass(ClassNode cNode, AnnotationNode anno) {
        if (!checkNotInterface(cNode, MY_TYPE_NAME)) {
            return
        }

        AnnotationConfig config = createAnnotationConfig(anno)
        if (hasAnnotation(cNode, CANONICAL_ANNOTATION)) {
            config.copyPropertiesFromCanonicalAST(cNode)
        }

        if (!checkIncludeExclude(anno, config.excludes, config.includes, MY_TYPE_NAME)) {
            return
        }

        createConstructor(cNode, config)
    }

    protected AnnotationConfig createAnnotationConfig(AnnotationNode anno) {
        return [
                includeFields         : memberHasValue(anno, "includeFields", true),
                includeProperties     : !memberHasValue(anno, "includeProperties", false),
                includeSuperFields    : memberHasValue(anno, "includeSuperFields", true),
                includeSuperProperties: memberHasValue(anno, "includeSuperProperties", true),
                callSuper             : memberHasValue(anno, "callSuper", true),
                force                 : memberHasValue(anno, "force", true),
                excludes              : getMemberList(anno, "excludes"),
                includes              : getMemberList(anno, "includes")
        ] as AnnotationConfig
    }

    static class AnnotationConfig {
        boolean includeFields,
                includeProperties,
                includeSuperFields,
                includeSuperProperties,
                callSuper,
                force

        List<String> includes,
                     excludes

        void copyPropertiesFromCanonicalAST(ClassNode cNode) {
            AnnotationNode canonical = cNode.getAnnotations(CANONICAL_ANNOTATION).get(0)
            if (excludes == null || excludes.isEmpty()) {
                excludes = getMemberList(canonical, "excludes")
            }
            if (includes == null || includes.isEmpty()) {
                includes = getMemberList(canonical, "includes")
            }
        }
    }

    static void createConstructor(ClassNode cNode, AnnotationConfig config) {
        // no processing if existing constructors found
        List<ConstructorNode> constructors = cNode.getDeclaredConstructors()
        if (constructors.size() > 1 && !config.force) return

        boolean foundEmpty = constructors.size() == 1 && constructors.get(0).getFirstStatement() == null
        if (constructors.size() == 1 && !foundEmpty && !config.force) return

        // HACK: JavaStubGenerator could have snuck in a constructor we don't want
        if (foundEmpty) constructors.remove(0)

        List<FieldNode> superList = []
        if (config.includeSuperProperties) {
            superList.addAll(getSuperPropertyFields(cNode.getSuperClass()))
        }
        if (config.includeSuperFields) {
            superList.addAll(getSuperNonPropertyFields(cNode.getSuperClass()))
        }

        List<FieldNode> list = []
        if (config.includeProperties) {
            list += getInstancePropertyFields(cNode)
        }
        if (config.includeFields) {
            list += getInstanceNonPropertyFields(cNode)
        }

        final List<Parameter> params = []
        final List<Expression> superParams = []
        final BlockStatement body = new BlockStatement()
        superList.each { FieldNode fNode ->
            if (shouldSkip(fNode.getName(), config.excludes, config.includes)) return

            String name = fNode.getName()
            params.add(createParam(fNode, name))
            if (config.callSuper) {
                superParams << varX(name)
            } else {
                body.addStatement(assignS(propX(varX("this"), name), varX(name)))
            }
        }
        if (config.callSuper) {
            body.addStatement(stmt(ctorX(ClassNode.SUPER, args(superParams))))
        }
        list.each { FieldNode fNode ->
            if (shouldSkip(fNode.getName(), config.excludes, config.includes)) return

            String name = fNode.getName()
            Parameter nextParam = createParam(fNode, name)
            params << nextParam
            body.addStatement(assignS(propX(varX("this"), name), varX(nextParam)))
        }
        cNode.addConstructor(
                new ConstructorNode(
                        ACC_PUBLIC,
                        params.toArray(new Parameter[params.size()]),
                        ClassNode.EMPTY_ARRAY, 
                        body
                )
        )
        // add map constructor if needed, don't do it for LinkedHashMap for now (would lead to duplicate signature)
        // or if there is only one Map property (for backwards compatibility)
        if (!params.isEmpty()) {
            ClassNode firstParam = params.get(0).getType()
            if (params.size() > 1 || firstParam == ClassHelper.OBJECT_TYPE) {
                if (firstParam == ClassHelper.MAP_TYPE) {
                    TupleConstructorASTTransformation.addMapConstructors(cNode, true, "The class " + cNode.getName() + " was incorrectly initialized via the map constructor with null.")
                } else {
                    ClassNode candidate = HMAP_TYPE
                    while (candidate != null) {
                        if (candidate == firstParam) {
                            TupleConstructorASTTransformation.addMapConstructors(cNode, true, "The class " + cNode.getName() + " was incorrectly initialized via the map constructor with null.")
                            break
                        }
                        candidate = candidate.getSuperClass()
                    }
                }
            }
        }
    }

    private static Parameter createParam(FieldNode fNode, String name) {
        return new Parameter(fNode.getType(), name)
    }

}

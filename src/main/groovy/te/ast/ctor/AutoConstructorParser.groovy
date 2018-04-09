package te.ast.ctor

import groovy.transform.CompileStatic
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.transform.AbstractASTTransformation

/**
 * Parses a class annotated with {@link AutoConstructor} into an {@link AutoConstructorSettings} object.
 *
 * <p>This class does not have unit tests since its logic is covered by the compile-time tests.
 */
@CompileStatic
class AutoConstructorParser {

    static AutoConstructorSettings parse(AnnotationNode annotation) {
        AutoConstructorSettings settings = [
                includeFields         : memberHasValue(annotation, "includeFields", true),
                includeProperties     : !memberHasValue(annotation, "includeProperties", false),
                includeSuperFields    : memberHasValue(annotation, "includeSuperFields", true),
                includeSuperProperties: memberHasValue(annotation, "includeSuperProperties", true),
                callSuper             : memberHasValue(annotation, "callSuper", true),
                force                 : memberHasValue(annotation, "force", true),
                addInjectAnnotation   : !memberHasValue(annotation, "addInjectAnnotation", false),
                excludes              : AbstractASTTransformation.getMemberList(annotation, "excludes"),
                includes              : AbstractASTTransformation.getMemberList(annotation, "includes")
        ]

        return settings
    }

    private static boolean memberHasValue(AnnotationNode node, String name, Object value) {
        Expression member = node.getMember(name)

        return member &&
                member instanceof ConstantExpression &&
                member.getValue() == value
    }

}

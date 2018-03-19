package te.ast.ctor.domain

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.ast.ClassNode
import te.ast.ctor.SingleConstructor

/**
 * Represents the values parsed from the {@link SingleConstructor} annotation.
 * Every function in that annotation will have a corresponding field here.
 */
@CompileStatic
@ToString(includePackage = false, includeNames = true)
class ASTParams {
    boolean includeFields,
            includeProperties,
            includeSuperFields,
            includeSuperProperties,
            callSuper,
            force

    List<String> includes,
                 excludes
}

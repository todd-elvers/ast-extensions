package te.ast.ctor.domain

import groovy.transform.CompileStatic
import groovy.transform.ToString
import te.ast.ctor.AutoConstructor

/**
 * Represents the values parsed from the {@link AutoConstructor} annotation.
 * Every function in that annotation will have a corresponding field here.
 */
@CompileStatic
@ToString(includePackage = false, includeNames = true)
class AnnotationSettings {
    boolean includeFields,
            includeProperties,
            includeSuperFields,
            includeSuperProperties,
            callSuper,
            force,
            addInjectAnnotation

    List<String> includes,
                 excludes
}

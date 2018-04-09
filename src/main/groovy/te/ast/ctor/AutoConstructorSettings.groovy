package te.ast.ctor

import groovy.transform.CompileStatic
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/**
 * Represents the values parsed from the {@link AutoConstructor} annotation.
 * Every function in that annotation will have a corresponding field here.
 */
@CompileStatic
@EqualsAndHashCode
@ToString(includePackage = false, includeNames = true)
class AutoConstructorSettings {
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

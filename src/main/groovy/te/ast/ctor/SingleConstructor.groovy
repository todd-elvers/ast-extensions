package te.ast.ctor

import org.codehaus.groovy.transform.GroovyASTTransformationClass

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * Name ideas:
 *  - @Constructor
 *  - @InjectConstructor
 *  - @AddConstructor
 *  - @NewConstructor
 *  - @AutomaticConstructor
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@GroovyASTTransformationClass(classes = [SingleConstructorASTTransformation])
@interface SingleConstructor {
    /**
     * List of field and/or property names to exclude from the constructor.
     * Must not be used if 'includes' is used. For convenience, a String with comma separated names
     * can be used in addition to an array (using Groovy's literal list notation) of String values.
     */
    String[] excludes() default []

    /**
     * List of field and/or property names to include within the constructor.
     * Must not be used if 'excludes' is used. For convenience, a String with comma separated names
     * can be used in addition to an array (using Groovy's literal list notation) of String values.
     */
    String[] includes() default []

    /**
     * Include fields in the constructor.
     */
    boolean includeFields() default false

    /**
     * Include properties in the constructor.
     */
    boolean includeProperties() default true

    /**
     * Include fields from super classes in the constructor.
     */
    boolean includeSuperFields() default false

    /**
     * Include properties from super classes in the constructor.
     */
    boolean includeSuperProperties() default false

    /**
     * Should super properties be called within a call to the parent constructor.
     * rather than set as properties
     */
    boolean callSuper() default false

    /**
     * By default, this annotation becomes a no-op if you provide your own constructor.
     * By setting {@code force=true} then the tuple constructor(s) will be added regardless of
     * whether existing constructors exist. It is up to you to avoid creating duplicate constructors.
     */
    boolean force() default false
}

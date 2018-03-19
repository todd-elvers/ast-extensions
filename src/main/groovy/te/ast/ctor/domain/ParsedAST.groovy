package te.ast.ctor.domain

import groovy.transform.CompileStatic
import groovy.transform.ToString
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.stmt.BlockStatement

/**
 * The result of parsing the AST of a class.
 *
 * <p>Used to contain the state while building up constructor args & method signature.
 */
@CompileStatic
@ToString(includePackage = false, includeNames = true)
class ParsedAST {
    List<Parameter> constructorArgs
    BlockStatement constructorBody

    Parameter[] toParamArray() {
        return constructorArgs.toArray(new Parameter[constructorArgs.size()])
    }
}

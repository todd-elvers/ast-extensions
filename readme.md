# AST Extensions

A project to store useful AST transformations I've made.

<br/>


## SingleConstructor

Field injection is no longer the recommended way to inject dependencies in Spring.  
The correct pattern now involves constructors, which are cumbersome and totally boilerplate.
This annotation seeks to solve this by automatically generating a single
constructor, without default values, that is annotated with `@Autowired`, or `@Inject`, based on the contents of the annotated class.
This way you can control how your objects are generated, don't have to look at clunky constructors,
and get to follow the best practice of constructor injection.

### Example

```groovy
@SingleConstructor
class Service {
    String propertyOne
    Boolean propertyTwo
}
```
would be transformed, at compile time, to
```groovy
class Service {
    String propertyOne
    Boolean propertyTwo
    
    Service(String propertyOne, Boolean propertyTwo) {
        this.propertyOne = propertyOne
        this.propertyTwo = propertyTwo
    }
}
``` 

<br/>
 

#### NOTE
No releases currently, this is still in alpha.
# AST Extensions

A project to store useful AST transformations I've made.

<br/>


## AutoConstructor

Field injection is no longer the recommended way to inject dependencies in Spring.  
The correct pattern now involves constructors, which are cumbersome boilerplate.
This annotation seeks to solve this by automatically generating a single
constructor, without default values, that is annotated with `@Inject`, and whose arguments are determined by the properties/fields of the class that was annotated.  This way you can control how your objects are generated, don't have to look at clunky constructors,
and get to follow the best practice of constructor injection.

### Example

```groovy
@AutoConstructor
class BookService {
    AuthorService authorService
    IdService idService
}
```
would be transformed, at compile time, to
```groovy
class BookService {
    AuthorService authorService
    IdService idService
    
    @Inject
    Service(AuthorService authorService, IdService idService) {
        this.authorService = authorService
        this.idService = idService
    }
}
``` 

<br/>
 

#### Progress
All features are working & the code is very heavily tested!
No release schedule currently available though.  Open an issue if you actually need this.

package app.rollvault.examplek

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExampleDocumentation(val body: String)

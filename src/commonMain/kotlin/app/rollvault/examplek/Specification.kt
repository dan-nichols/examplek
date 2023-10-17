package app.rollvault.examplek

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Specification(
    val title: String,
    val description: String = "",

    /**
     * Omit .md extension
     */
    val filename: String = "",
)

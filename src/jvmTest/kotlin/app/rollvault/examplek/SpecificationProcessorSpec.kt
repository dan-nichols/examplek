package app.rollvault.examplek

import com.tschuchort.compiletesting.*
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private const val KOTLIN_VERSION = "1.9.10"
private const val REPO = "rollvault/examplek"
private const val REPO_LOCATION = "https://github.com/$REPO"
private const val SHIELDS = "https://img.shields.io"
private const val BADGE = "$SHIELDS/badge"
private const val CICD_LINK = "$REPO_LOCATION/actions/workflows/continuous-deployment.yml"

private const val COMMON_IMPORTS = """
    package app.rollvault.examplek
        
    import kotlin.test.assertEquals
    import app.rollvault.examplek.Specification
    import app.rollvault.examplek.Example
    import app.rollvault.examplek.ExampleDocumentation
"""

@Specification(
    filename = "README",
    title = "ExampleK",
    description = """
[![Kotlin]($BADGE/Kotlin-$KOTLIN_VERSION-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![License]($SHIELDS/github/license/$REPO)]($REPO_LOCATION/blob/main/LICENSE)
[![Github Actions]($CICD_LINK/badge.svg)]($CICD_LINK)

![badge-android]($BADGE/-android-6EDB8D.svg?style=flat)
![badge-jvm]($BADGE/-jvm-DB413D.svg?style=flat)
![badge-ios]($BADGE/-ios-CDCDCD.svg?style=flat)

ExampleK lets you generate markdown documentation directly from your tests, ensuring both the tests, and the documentation share a single source of truth.

It is a Kotlin Symbol Processor (KSP) for Kotlin Multiplatform that uses concrete examples from parameterised tests to generate documentation, ensuring that the docs are always up to date.

Get started by adding the gradle plugin to your project:

```
TODO: Add maven central and instructions
```
"""
)
class SpecificationProcessorSpec {

    @ExampleDocumentation(
        """
        ---
        ### Generating markdown specifications from tests
        
        Classes annotated with `@Specification` will generate markdown documentation. This can be used to configure the markdown filename, and documentation at the top of the file.

        `@Example` with no arguments generates a test with the body of the annotated function. 
        
        `@ExampleDocumentation` is used to configure the markdown body for the example.

        This example describes a spec for String.length behaviour in plain English.

        ```kotlin
        %s
        ```
        
        Running `./gradlew check` will output these file contents in `build/generated/ksp/{sourceSet}/{sourceSetTest}/%s` for every `sourceSet` the tests are run on:
        
        %s
    """
    )
    @Example(
        """
    $COMMON_IMPORTS

    @Specification(
        title = "String Specification",
        description = "This spec documents string length.",
        filename = "MyStringSpec"
    )
    class StringSpec {
    
        @ExampleDocumentation("A blank string has a length of zero.")
        @Example
        fun verifyBlankStringLength() {
            assertEquals(0, "".length)
        }
    }
    """,
        "resources/specs/MyStringSpec.md",
        """
    > # String Specification
    > 
    > This spec documents string length.
    > 
    > A blank string has a length of zero.
    """
    )
    fun testSpecification(specSource: String, expectedGeneratedFilename: String, expectedMarkdown: String) {
        val generatedMarkdown = testGeneration(specSource, expectedGeneratedFilename)

        assertEquals(expectedMarkdown.replace("> ", ""), generatedMarkdown)
    }

    @ExampleDocumentation(
        """
        Of course an example could use the given-when-then format instead:

        ```kotlin
        %s
        ```
        
        The additional example generates a new line in the same spec:
        
        %s
    """
    )
    @Example(
        """
        @ExampleDocumentation("Given a blank string, when calculating its length, then the length will be 0.")
        @Example
        fun verifyBlankStringLength() {
            assertEquals(0, "".length)
        }
        """,
        "> Given a blank string, when calculating its length, then the length will be 0."
    )
    fun givenWhenThenExample(specSource: String, expectedMarkdown: String) =
        testSingleFunction(specSource, expectedMarkdown)

    @ExampleDocumentation(
        """
        ### Multiple Examples

        Using multiple `@Example` annotations on a function allows it to be run as a parameterised test.
        
        Arguments are passed as a `vararg` of Strings. Each example must have the same number of `vararg` args as the function it annotates.
        
        Currently only String arguments are supported.

        ```kotlin
        %s
        ```
        
        Java string format can be used to interpolate args for use in the `@ExampleDocumentation`. The `@ExampleDocumentation` body will be printed for every `@Example` on the function.
        
        %s
    """
    )
    @Example(
        """
        @ExampleDocumentation("%s is a three letter word.")
        @Example("Foo")
        @Example("Bar")
        @Example("Baz")
        fun testValidThreeLetterWords(word: String) {
            assertEquals(3, word.length)
        }
        """,
        """
            > Foo is a three letter word.
            > 
            > Bar is a three letter word.
            > 
            > Baz is a three letter word.
        """
    )
    fun multipleExamples(specSource: String, expectedMarkdown: String) =
        testSingleFunction(specSource, expectedMarkdown)

    @ExampleDocumentation(
        """
        Here's an example of multiple arguments being formatted:

        ```kotlin
        %s
        ```
        
        Generating:
        
        %s
    """
    )
    @Example(
        """
        @ExampleDocumentation("%s is a %s letter word.")
        @Example("Foo", "3")
        @Example("Food", "4")
        fun testValidNLetterWords(word: String, expectedLength: String) {
            assertEquals(expectedLength.toInt(), word.length)
        }
        """,
        """
            > Foo is a 3 letter word.
            > 
            > Food is a 4 letter word.
        """
    )
    fun multipleArguments(specSource: String, expectedMarkdown: String) =
        testSingleFunction(specSource, expectedMarkdown)

    @ExampleDocumentation(
        """
        ### Format examples as lists

        Examples can be formatted as markdown lists by using the `@ExamplesList` annotation. Add it in addition to the `@Example`s.
        
        By default, the first argument will be formatted in the list as-is. Other arguments will be ignored in documentation, but still used for parameterised tests.

        ```kotlin
        %s
        ```
        
        For functions with `@ExamplesList`, the `@ExampleDocumentation` body will only be printed once.
        
        %s
    """
    )
    @Example(
        """
        @ExampleDocumentation("The following are three letter words:")
        @ExamplesList
        @Example("Foo", "3")
        @Example("Bar", "3")
        @Example("Baz", "3")
        fun testValidThreeLetterWordsAsList(word: String, expectedLength: String) {
            assertEquals(expectedLength.toInt(), word.length)
        }
        """,
        """
            > The following are three letter words:
            > 
            > * Foo
            > * Bar
            > * Baz
        """
    )
    fun listExamples(specSource: String, expectedMarkdown: String) =
        testSingleFunction(specSource, expectedMarkdown)

    @ExampleDocumentation(
        """
        A custom String format can also be provided to for lists that will be applied individually to each list item.

        ```kotlin
        %s
        ```
        
        Generates:
        
        %s
    """
    )
    @Example(
        """
        @ExampleDocumentation("Example words have different lengths:")
        @ExamplesList("%s is a %s letter word")
        @Example("Foo", "3")
        @Example("Food", "4")
        @Example("Flood", "5")
        fun testValidNLetterWordsAsList(word: String, expectedLength: String) {
            assertEquals(expectedLength.toInt(), word.length)
        }
        """,
        """
            > Example words have different lengths:
            > 
            > * Foo is a 3 letter word
            > * Food is a 4 letter word
            > * Flood is a 5 letter word
        """
    )
    fun formattedListExamples(specSource: String, expectedMarkdown: String) =
        testSingleFunction(specSource, expectedMarkdown)

    @ExampleDocumentation(
        """
        ### Format examples as tables

        Examples can be formatted as markdown tables by using the `@ExamplesTable` annotation. Add it in addition to the `@Example`s.
        
        An `ExampleColumn` vararg is required for each example argument to provide column names, and optional formatting.

        ```kotlin
        %s
        ```
        
        For functions with `@ExamplesTable`, the `@ExampleDocumentation` body will only be printed once.
        
        %s
    """
    )
    @Example(
        """
        @ExampleDocumentation("Example words have different lengths:")
        @ExamplesTable(
            ExampleColumn("Word"),
            ExampleColumn(
                name = "# Letters", 
                format = "**%s**", 
                alignment = ColumnAlignment.CENTER, 
            ),
        )
        @Example("Foo", "3")
        @Example("Food", "4")
        @Example("Flood", "5")
        fun testValidNLetterWordsAsTable(word: String, expectedLength: String) {
            assertEquals(expectedLength.toInt(), word.length)
        }
        """,
        """
            > Example words have different lengths:
            
            | Word | # Letters |
            | --- | :---: |
            | Foo | **3** |
            | Food | **4** |
            | Flood | **5** |
        """
    )
    fun tableExamples(specSource: String, expectedMarkdown: String) {
        val generatedMarkdown = testGeneration(
            specSource = """
                $COMMON_IMPORTS
                
                @Specification("String Specification")
                class StringSpec {
        
                    $specSource
                }
            """.trimIndent(),
            expectedGeneratedFilename = "resources/specs/StringSpec.md",
        )

        assertEquals(
            expectedMarkdown.replace("> ", ""), generatedMarkdown.substringAfter(
                """
                    # String Specification
                    
                    
                """.trimIndent()
            )
        )
    }

    @ExampleDocumentation(
        """
        Using both `@ExamplesTable` and `@ExamplesList` is not possible, and will throw an error. The below example is invalid:

        ```kotlin
        %s
        ```
    """
    )
    @Example(
        """
        // This example is INVALID!
        
        @ExampleDocumentation("Example words have different lengths:")
        @ExamplesList("%s is a %s letter word")
        @ExamplesTable(ExampleColumn("Word"), ExampleColumn("Letters"))
        @Example("Foo", "3")
        @Example("Food", "4")
        fun invalidTest(word: String, expectedLength: String) {
            assertEquals(expectedLength.toInt(), word.length)
        }
        """,
    )
    fun usingBothListAndTableFails(specSource: String) {
        assertFailsWith<AssertionError> {
            testSingleFunction(specSource, "anything")
        }
    }

    private fun testSingleFunction(specSource: String, expectedMarkdown: String) {
        val generatedMarkdown = testGeneration(
            specSource = """
                $COMMON_IMPORTS
                
                @Specification("String Specification")
                class StringSpec {
        
                    $specSource
                }
            """.trimIndent(),
            expectedGeneratedFilename = "resources/specs/StringSpec.md",
        )

        assertEquals(
            expectedMarkdown.replace("> ", ""), generatedMarkdown.substringAfter(
                """
                    # String Specification
                    
                    
                """.trimIndent()
            )
        )
    }

    private fun testGeneration(
        specSource: String,
        expectedGeneratedFilename: String,
    ): String {
        val source = SourceFile.kotlin("StringSpec.kt", specSource)

        val compilation = KotlinCompilation().apply {
            sources = listOf(source)
            symbolProcessorProviders = listOf(SpecificationProcessorProvider())
            inheritClassPath = true
            kspIncremental = true
        }
        val result = compilation.compile()

        assertEquals(KotlinCompilation.ExitCode.OK, result.exitCode)

        val generatedMarkdown = result.kspGeneratedSources.first { expectedGeneratedFilename.endsWith(it.name) }
        val generatedTest = result.kspGeneratedSources.first { it.name == "StringSpecTest.kt" }

        assertEquals(expectedGeneratedFilename, generatedMarkdown.path.substringAfter("ksp/sources/"))
        assertTrue(generatedTest.readText().contains("private val specification = StringSpec()"))

        return generatedMarkdown.readText().trimIndent().trimIndent()
    }

    private val KotlinCompilation.Result.workingDir: File
        get() = outputDirectory.parentFile!!

    private val KotlinCompilation.Result.kspGeneratedSources: List<File>
        get() {
            val kspWorkingDir = workingDir.resolve("ksp")
            val kspGeneratedDir = kspWorkingDir.resolve("sources")
            val kotlinGeneratedDir = kspGeneratedDir.resolve("kotlin")
            val resourcesGeneratedDir = kspGeneratedDir.resolve("resources")
            return kotlinGeneratedDir.walkTopDown().toList() + resourcesGeneratedDir.walkTopDown()
        }
}

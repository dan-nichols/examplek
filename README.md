# ExampleK

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.10-blue.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/github/license/dan-nichols/examplek)](https://github.com/dan-nichols/examplek/blob/main/LICENSE)
[![Github Actions](https://github.com/dan-nichols/examplek/actions/workflows/continuous-deployment.yml/badge.svg)](https://github.com/dan-nichols/examplek/actions/workflows/continuous-deployment.yml)

![badge-android](https://img.shields.io/badge/-android-6EDB8D.svg?style=flat)
![badge-jvm](https://img.shields.io/badge/-jvm-DB413D.svg?style=flat)
![badge-ios](https://img.shields.io/badge/-ios-CDCDCD.svg?style=flat)

ExampleK lets you generate markdown documentation directly from your tests, ensuring both the tests, and the
documentation share a single source of truth.

It is a Kotlin Symbol Processor (KSP) for Kotlin Multiplatform that uses concrete examples from parameterised tests to
generate documentation, ensuring that the docs are always up to date.

Get started by adding the gradle plugin to your project:

```
TODO: Add maven central and instructions
```

---

### Generating markdown specifications from tests

Classes annotated with `@Specification` will generate markdown documentation. This can be used to configure the markdown
filename, and documentation at the top of the file.

`@Example` with no arguments generates a test with the body of the annotated function.

`@ExampleDocumentation` is used to configure the markdown body for the example.

This example describes a spec for String.length behaviour in plain English.

```kotlin

package app.rollvault.examplek

import kotlin.test.assertEquals
import app.rollvault.examplek.Specification
import app.rollvault.examplek.Example
import app.rollvault.examplek.ExampleDocumentation


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
```

Running `./gradlew check` will output these file contents
in `build/generated/ksp/{sourceSet}/{sourceSetTest}/resources/specs/MyStringSpec.md` for every `sourceSet` the tests are
run on:

> # String Specification
>
> This spec documents string length.
>
> A blank string has a length of zero.

Of course an example could use the given-when-then format instead:

```kotlin
@ExampleDocumentation("Given a blank string, when calculating its length, then the length will be 0.")
@Example
fun verifyBlankStringLength() {
    assertEquals(0, "".length)
}
```

The additional example generates a new line in the same spec:

> Given a blank string, when calculating its length, then the length will be 0.

### Multiple Examples

Using multiple `@Example` annotations on a function allows it to be run as a parameterised test.

Arguments are passed as a `vararg` of Strings. Each example must have the same number of `vararg` args as the function
it annotates.

Currently only String arguments are supported.

```kotlin
@ExampleDocumentation("%s is a three letter word.")
@Example("Foo")
@Example("Bar")
@Example("Baz")
fun testValidThreeLetterWords(word: String) {
    assertEquals(3, word.length)
}
```

Java string format can be used to interpolate args for use in the `@ExampleDocumentation`. The `@ExampleDocumentation`
body will be printed for every `@Example` on the function.

> Foo is a three letter word.
>
> Bar is a three letter word.
>
> Baz is a three letter word.

Here's an example of multiple arguments being formatted:

```kotlin
@ExampleDocumentation("%s is a %s letter word.")
@Example("Foo", "3")
@Example("Food", "4")
fun testValidNLetterWords(word: String, expectedLength: String) {
    assertEquals(expectedLength.toInt(), word.length)
}
```

Generating:

> Foo is a 3 letter word.
>
> Food is a 4 letter word.

### Format examples as lists

Examples can be formatted as markdown lists by using the `@ExamplesList` annotation. Add it in addition to
the `@Example`s.

By default, the first argument will be formatted in the list as-is. Other arguments will be ignored in documentation,
but still used for parameterised tests.

```kotlin
@ExampleDocumentation("The following are three letter words:")
@ExamplesList
@Example("Foo", "3")
@Example("Bar", "3")
@Example("Baz", "3")
fun testValidThreeLetterWordsAsList(word: String, expectedLength: String) {
    assertEquals(expectedLength.toInt(), word.length)
}
```

For functions with `@ExamplesList`, the `@ExampleDocumentation` body will only be printed once.

> The following are three letter words:
>
> * Foo
> * Bar
> * Baz

A custom String format can also be provided to for lists that will be applied individually to each list item.

```kotlin
@ExampleDocumentation("Example words have different lengths:")
@ExamplesList("%s is a %s letter word")
@Example("Foo", "3")
@Example("Food", "4")
@Example("Flood", "5")
fun testValidNLetterWordsAsList(word: String, expectedLength: String) {
    assertEquals(expectedLength.toInt(), word.length)
}
```

Generates:

> Example words have different lengths:
>
> * Foo is a 3 letter word
> * Food is a 4 letter word
> * Flood is a 5 letter word

### Format examples as tables

Examples can be formatted as markdown tables by using the `@ExamplesTable` annotation. Add it in addition to
the `@Example`s.

An `ExampleColumn` vararg is required for each example argument to provide column names, and optional formatting.

```kotlin
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
```

For functions with `@ExamplesTable`, the `@ExampleDocumentation` body will only be printed once.

> Example words have different lengths:

| Word  | # Letters |
|-------|:---------:|
| Foo   |   **3**   |
| Food  |   **4**   |
| Flood |   **5**   |

Using both `@ExamplesTable` and `@ExamplesList` is not possible, and will throw an error. The below example is invalid:

```kotlin
// This example is INVALID!

@ExampleDocumentation("Example words have different lengths:")
@ExamplesList("%s is a %s letter word")
@ExamplesTable(ExampleColumn("Word"), ExampleColumn("Letters"))
@Example("Foo", "3")
@Example("Food", "4")
fun invalidTest(word: String, expectedLength: String) {
    assertEquals(expectedLength.toInt(), word.length)
}
```


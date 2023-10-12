package com.dannichols.examplek

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ExamplesTable(vararg val columns: ExampleColumn)

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ExampleColumn(
    val name: String,
    val format: String = "%s",
    val alignment: ColumnAlignment = ColumnAlignment.UNDEFINED,
)

enum class ColumnAlignment {
    LEFT, RIGHT, CENTER, UNDEFINED
}

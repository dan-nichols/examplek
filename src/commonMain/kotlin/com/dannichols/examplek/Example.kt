package com.dannichols.examplek

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
@Repeatable
annotation class Example(
    vararg val args: String,
)

package com.dannichols.examplek

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class Specification(
    val title: String,
    val description: String = "",
)

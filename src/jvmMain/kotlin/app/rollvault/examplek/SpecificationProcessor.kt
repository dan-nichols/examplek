package app.rollvault.examplek

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.validate

class SpecificationProcessor(private val environment: SymbolProcessorEnvironment) : SymbolProcessor {

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val specs = resolver.getSymbolsWithAnnotation(Specification::class.qualifiedName.toString())
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.containingFile != null }
        if (!specs.iterator().hasNext()) return emptyList()

        specs.forEach { spec ->
            buildParameterisedTests(spec)
            buildMarkdownDocumentation(spec)
        }

        return specs.filterNot { it.validate() }.toList()
    }

    @OptIn(KspExperimental::class)
    private fun buildParameterisedTests(specClass: KSClassDeclaration) {
        val packageName = specClass.packageName.asString()
        val specClassName = specClass.simpleName.asString()
        val testName = "${specClassName}Test"
        val exampleGroups = specClass.getAllFunctions().filter { it.isAnnotationPresent(Example::class) }

        val testText = buildString {
            appendLn("package $packageName")

            newLine()

            appendLn("import kotlin.test.Test")
            newLine()

            appendLn("class $testName {")
            newLine()
            appendLn("    private val specification = $specClassName()")
            newLine()

            exampleGroups.forEach { exampleGroup ->
                val name = exampleGroup.getName()

                val examples = exampleGroup.getAnnotationsByType(Example::class)

                if (exampleGroup.parameters.isEmpty()) {
                    // Ignore the arg annotations. TODO: Warn when the Example annotation has args and we're ignoring them?
                    appendTest(name, name)
                } else {
                    examples.forEachIndexed { index, example ->
                        // TODO: Figure out how to provide error handling that points you to the exact offending line
                        if (exampleGroup.parameters.size != example.args.size) throw IllegalArgumentException("Number of arguments in `Example` annotation does not match number of function arguments")

                        // Regex checks if arguments contain any illegal characters for kotlin functions
                        val testSuffix = if (examples.toList().size == 1) {
                            ""
                        } else if (example.args.all { it.matches(Regex("^[^ !\"#\$%^&()*+,-=?@_{|}~]+\$")) }) {
                            example.args.joinToString("")
                        } else {
                            "$index"
                        }

                        appendTest(exampleGroup.getName(testSuffix), name, example.joinToString())
                    }
                }
            }

            appendLn("}")
        }

        specClass.createKtFile(packageName, testName, testText)
    }

    @OptIn(KspExperimental::class)
    private fun buildMarkdownDocumentation(specClass: KSClassDeclaration) {
        val spec = specClass.getAnnotationsByType(Specification::class).first()
        val exampleGroups = specClass.getAllFunctions().filter { it.isAnnotationPresent(ExampleDocumentation::class) }

        val markdownText = buildString {
            appendLn("# ${spec.title}")

            newLine()

            if (spec.description.isNotBlank()) {
                appendLn(spec.description.trimIndent())
                newLine()
            }

            exampleGroups.forEach { exampleGroup ->
                val exampleGroupsDocs = exampleGroup.getAnnotationsByType(ExampleDocumentation::class).first()

                val examplesList = exampleGroup.getAnnotationsByType(ExamplesList::class).firstOrNull()
                val examplesTable = exampleGroup.getAnnotationsByType(ExamplesTable::class).firstOrNull()

                val examples = exampleGroup.getAnnotationsByType(Example::class)

                if (examplesList != null && examplesTable != null) {
                    throw IllegalArgumentException("Cannot format examples as both table and list")
                } else if (examplesList != null) {
                    // The example group documentation is printed at the top, followed by a bullet list of each example.
                    // List items are formatted with the example arguments, but the group-level documentation is not.

                    appendLn(exampleGroupsDocs.body.trimIndent())
                    newLine()

                    examples.forEach { example ->
                        appendLn("* ${examplesList.itemTemplate.format(*example.args)}")
                    }
                    newLine()
                } else if (examplesTable != null) {
                    // The example group documentation is printed at the top, followed by a table of all examples.
                    // Table headings should match example arguments.
                    // Group-level documentation is not string formatted.
                    val LINE = "|"

                    examples.forEach { example ->
                        if (example.args.size != examplesTable.columns.size) throw IllegalArgumentException("Number of table column headers must match number of example arguments")
                    }

                    appendLn(exampleGroupsDocs.body.trimIndent())
                    newLine()

                    val tableHeader = examplesTable.columns.fold(LINE) { acc, column ->
                        "$acc ${column.name} $LINE"
                    }
                    appendLn(tableHeader)

                    val tableSeparator = examplesTable.columns.fold(LINE) { acc, column ->
                        "$acc ${
                            when (column.alignment) {
                                ColumnAlignment.LEFT -> ":---"
                                ColumnAlignment.RIGHT -> "---:"
                                ColumnAlignment.CENTER -> ":---:"
                                ColumnAlignment.UNDEFINED -> "---"
                            }
                        } $LINE"
                    }
                    appendLn(tableSeparator)

                    examples.forEach { example ->
                        // Should we rely only on string format index for example column ordering?
                        val row = example.args.foldIndexed(LINE) { index, acc, argument ->
                            "$acc ${examplesTable.columns[index].format.format(argument)} $LINE"
                        }

                        appendLn(row)
                    }
                    newLine()
                } else {
                    // Each Example's documentation string is printed on a new line of the markdown

                    examples.forEach { example ->
                        appendLn(
                            exampleGroupsDocs.body
                                .trimIndent()
                                .format(
                                    *example.args
                                        .map(String::trimIndent)
                                        .toTypedArray()
                                )
                                .trimIndent()
                        )
                        newLine()
                    }
                }
            }
        }

        val filename: String = spec.filename.ifBlank {
            specClass.simpleName.asString()
        }

        specClass.createMdFile("specs/$filename", markdownText)
    }

    private fun KSClassDeclaration.createKtFile(packageName: String, className: String, text: String) {
        val file = environment.codeGenerator.createNewFile(
            Dependencies(false, containingFile!!),
            packageName,
            className
        )
        file.write(text.toByteArray())
    }

    private fun KSClassDeclaration.createMdFile(path: String, text: String) {
        val file = environment.codeGenerator.createNewFileByPath(
            Dependencies(false, containingFile!!),
            path,
            "md"
        )
        file.write(text.toByteArray())
    }

    private fun KSFunctionDeclaration.getName(suffix: String = ""): String {
        val name = "${simpleName.asString()}$suffix"

        return if (name.contains(" ")) {
            "`$name`"
        } else {
            name
        }
    }

    private fun StringBuilder.newLine(count: Int = 1) {
        repeat(count) {
            append("\n")
        }
    }

    private fun StringBuilder.appendLn(str: String) {
        append(str)
        newLine()
    }

    private fun StringBuilder.appendTest(testName: String, functionName: String, arguments: String = "") {
        appendLn("    @Test")
        appendLn("    fun $testName() {")
        appendLn("        specification.$functionName($arguments)")
        appendLn("    }")
        newLine()
    }

    private fun Example.joinToString() =
        args.foldIndexed("") { argIndex, accumulation, argument ->
            if (argIndex == 0) {
                "\"\"\"${argument.trimIndent()}\"\"\""
            } else {
                "$accumulation, \"\"\"${argument.trimIndent()}\"\"\""
            }
        }
}

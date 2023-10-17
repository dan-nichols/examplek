package app.rollvault.examplek

import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider

class SpecificationProcessorProvider : SymbolProcessorProvider {
    
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return SpecificationProcessor(environment)
    }
}

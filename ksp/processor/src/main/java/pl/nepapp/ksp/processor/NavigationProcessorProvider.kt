package pl.nepapp.ksp.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import pl.nepapp.ksp.annotations.Screen

class NavigationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NavigationProcessorProcessor(
            environment.codeGenerator,
            environment.logger,
            options = environment.options
        )
    }
}

private var processed: Boolean = false

class NavigationProcessorProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>
) : SymbolProcessor {

    private val myScreenQualifiedName = "pl.nepapp.coreui.test.MyScreen"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(Screen::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()

        if (!symbols.iterator().hasNext()) return emptyList()

        val fileSpecBuilder = FileSpec.builder("com.example.generated", "GeneratedNavHost")

        fileSpecBuilder.addFunction(generateNavHostFunction(symbols))

        val fileSpec = fileSpecBuilder.build()
        fileSpec.writeTo(codeGenerator, Dependencies(false))

        return emptyList()
    }

    private fun generateNavHostFunction(symbols: Sequence<KSFunctionDeclaration>): FunSpec {
        val funSpecBuilder = FunSpec.builder("NavigationGraph")
            .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
            .addParameter("initialScreen", ClassName("pl.nepapp.kspproject", "Direction"))
            .addParameter(
            //    "%T", ClassName("androidx.navigation", "NavHostController")
                ParameterSpec.builder("navHostController", ClassName("androidx.navigation", "NavHostController"))
                    .defaultValue("%T()", ClassName("androidx.navigation.compose", "rememberNavController"))
                    .build()
            )
            .addCode("return %T(\n", ClassName("pl.nepapp.kspproject", "BaseNavHost"))
            .addCode("startDestination = initialScreen,\n")
            .addCode("navController = navHostController\n")
            .addCode(") {\n")

        symbols.forEach { symbol ->
            val annotation = symbol.annotations.first { it.shortName.asString() == "Screen" }
            val direction = annotation.arguments.first().value as KSType
            val directionName = direction.toClassName().canonicalName

            funSpecBuilder.addCode(
                "%T<$directionName> {\n",
                ClassName("androidx.navigation.compose", "composable")
            )
            funSpecBuilder.addCode("${symbol.qualifiedName!!.asString()}()\n")
            funSpecBuilder.addCode("}\n")
        }

        funSpecBuilder.addCode("}\n")

        return funSpecBuilder.build()
    }
}

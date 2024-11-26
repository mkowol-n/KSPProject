package pl.nepapp.ksp.processor

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import pl.nepapp.ksp.annotations.Screen

class NavigationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NavigationProcessorProcessor(
            environment.codeGenerator,
        )
    }
}

class NavigationProcessorProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private val composableAnnotationClassName = ClassName("androidx.compose.runtime", "Composable")
    private val rememberNavControllerMemberName = MemberName("androidx.navigation.compose", "rememberNavController")
    private val navHostControllerClassName = ClassName("androidx.navigation", "NavHostController")
    private val composableMember = MemberName("androidx.navigation.compose", "composable")

    private val directionClassName = ClassName("pl.nepapp.kspproject", "Direction")  // Tu wrzucasz package swojego direction interface
    private val baseNavHostClassName = ClassName("pl.nepapp.kspproject", "BaseNavHost")  // Tu wrzucasz package BaseNavHosta

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
            .addAnnotation(composableAnnotationClassName)
            .addParameter("initialScreen", directionClassName)
            .addParameter(
                ParameterSpec.builder("navHostController", navHostControllerClassName)
                    .defaultValue("%M()", rememberNavControllerMemberName)
                    .build()
            )
            .addCode("return %T(\n", baseNavHostClassName)
            .addCode("startDestination = initialScreen,\n")
            .addCode("navController = navHostController\n")
            .addCode(") {\n")

        symbols.forEach { symbol ->
            val annotation = symbol.annotations.first {
                it.annotationType.resolve().toClassName() == Screen::class.asTypeName()
            }
            val direction = annotation.arguments.first { it.name?.getShortName() == Screen::direction.name }.value as KSType

            val directionName = direction.toClassName().canonicalName
            val screenName = requireNotNull(symbol.qualifiedName).asString()

            funSpecBuilder.addCode("%M<$directionName> {\n", composableMember)
            funSpecBuilder.addCode("$screenName()\n")
            funSpecBuilder.addCode("}\n")
        }

        funSpecBuilder.addCode("}\n")

        return funSpecBuilder.build()
    }
}

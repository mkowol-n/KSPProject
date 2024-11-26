package pl.nepapp.ksp.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toKModifier
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import pl.nepapp.ksp.annotations.ScreenRegistry

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
    private val rememberNavControllerMemberName =
        MemberName("androidx.navigation.compose", "rememberNavController")
    private val navHostControllerClassName = ClassName("androidx.navigation", "NavHostController")

    private val directionClassName = ClassName(
        "pl.nepapp.kspproject",
        "Direction"
    )  // Tu wrzucasz package swojego direction interface
    private val baseNavHostClassName =
        ClassName("pl.nepapp.kspproject", "BaseNavHost")  // Tu wrzucasz package BaseNavHosta
    private val baseComposableRegistrator = MemberName(
        "pl.nepapp.kspproject",
        "registerBaseComposable"
    )  // Tu wrzucasz i name swojej głównej nawigacji compose
    private val directionTypeMapCompanionClassName =
        ClassName("pl.nepapp.kspproject", "DirectionTypeMapCompanion")
    private val generatedModulePackageName = "pl.nepapp.kspproject"
    private val nameOfNavigationComposable = "GeneratedNavHost"

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ScreenRegistry::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()

        if (!symbols.iterator().hasNext()) return emptyList()

        resolver.getAllFiles().first().packageName
        val fileSpecBuilder =
            FileSpec.builder(generatedModulePackageName, nameOfNavigationComposable)

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
                it.annotationType.resolve().toClassName() == ScreenRegistry::class.asTypeName()
            }
            val direction =
                annotation.arguments.first { it.name?.getShortName() == ScreenRegistry::direction.name }.value as KSType

            val directionName = direction.toClassName().canonicalName
            val screenName = requireNotNull(symbol.qualifiedName).asString()

            funSpecBuilder.addCode("%M<$directionName>", baseComposableRegistrator)

            val direcionCompanionObject =
                (direction.declaration as KSClassDeclaration).declarations.filterIsInstance<KSClassDeclaration>()
                    .firstOrNull { it.isCompanionObject }
            if (direcionCompanionObject != null) {

                if (direcionCompanionObject.superTypes.none { it.resolve().toClassName() == directionTypeMapCompanionClassName }) {
                    throw Exception("Companion object of $directionName must be type of ${directionTypeMapCompanionClassName.canonicalName}")
                }

                val typeMapName = direcionCompanionObject.getDeclaredProperties().first()

                funSpecBuilder.addCode("(typeMap = ${directionName}.$typeMapName )")
            }

            funSpecBuilder.addCode("{\n")
            funSpecBuilder.addCode("$screenName()\n")
            funSpecBuilder.addCode("}\n")
        }

        funSpecBuilder.addCode("}\n")

        return funSpecBuilder.build()
    }
}

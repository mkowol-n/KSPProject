package pl.nepapp.ksp.processor

import com.google.devtools.ksp.getDeclaredProperties
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.google.devtools.ksp.symbol.Modifier
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import pl.nepapp.ksp.annotations.ScreenRegistry

class NavigationProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return NavigationProcessorProcessor(
            environment.codeGenerator,
        )
    }
}

//Map<KType, @JvmSuppressWildcards NavType<*>>
class NavigationProcessorProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private val composableAnnotationClassName = ClassName("androidx.compose.runtime", "Composable")
    private val rememberNavControllerMemberName =
        MemberName("androidx.navigation.compose", "rememberNavController")
    private val navHostControllerClassName = ClassName("androidx.navigation", "NavHostController")
    private val typeOfMemberName = MemberName("kotlin.reflect", "typeOf")
    private val serializableTypeMemberName = MemberName("pl.nepapp.kspproject", "serializableType")
    private val navigationSavedStateHandleClassName =
        ClassName("pl.nepapp.kspproject", "NavigationSavedStateHandle")

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
    private val directionTypeMapCompanionClassName = ClassName(
        "pl.nepapp.kspproject",
        "DirectionTypeMapCompanion"
    ) // Tu wrzucasz to co mam w direction jaok DirectionTypeMapCompanion
    private val generatedModulePackageName = "pl.nepapp.kspproject" // twój app module package name
    private val nameOfNavigationComposable =
        "GeneratedNavHost" // to jak ma sie nazywac wygenerowany plik

    val navTypeClassName = ClassName("androidx.navigation", "NavType")
    val kTypeClassName = ClassName("kotlin.reflect", "KType")

    val mapType = Map::class.asClassName().parameterizedBy(
        kTypeClassName,
        navTypeClassName.parameterizedBy(STAR)
            .copy(annotations = listOf(AnnotationSpec.builder(JvmSuppressWildcards::class).build()))
    )

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

            funSpecBuilder.buildScreenRegistry(
                annotation = annotation,
                directionName = directionName
            )

            funSpecBuilder.addNavTypesIfNeeded(
                direction = direction,
                directionName = directionName,
                codeGenerator = codeGenerator
            )

            funSpecBuilder.addCode("{\n")
            funSpecBuilder.addCode("$screenName()\n")
            funSpecBuilder.addCode("}\n")
        }

        funSpecBuilder.addCode("}\n")

        return funSpecBuilder.build()
    }

    private fun FunSpec.Builder.addNavTypesIfNeeded(
        direction: KSType,
        directionName: String,
        codeGenerator: CodeGenerator
    ): FunSpec.Builder {

        val listOfObjects = mutableListOf<KSType>()
        (direction.declaration as KSClassDeclaration).getAllProperties().forEach { field ->
            val fieldResolver = field.type.resolve()
            if (isDataClass(fieldResolver) && listOfObjects.none { it.toClassName().canonicalName == fieldResolver.toClassName().canonicalName  } ) {
                listOfObjects.add(fieldResolver)
            }
        }


        if (listOfObjects.isEmpty()) {
            return this
        }

        val mapItems = listOfObjects.joinToString("", transform = ::getReturnNavTypeStatement)


        FileSpec.builder(generatedModulePackageName, "${directionName}Extension")
            .addProperty(
                PropertySpec.builder("navType", mapType)
                    .receiver(direction.toClassName().nestedClass("Companion"))
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return mapOf ( $mapItems )")
                            .build()
                    )
                    .build()
            )

            .addFunction(
                FunSpec.builder("getDirection")
                    .receiver(direction.toClassName().nestedClass("Companion"))
                    .addParameter(
                        ParameterSpec.builder(
                            "navigationSavedStateHandle", navigationSavedStateHandleClassName
                        ).build()
                    )
                    .returns(direction.toClassName())
                    .addStatement("return navigationSavedStateHandle.getDirection(${direction.toClassName().canonicalName}::class, ${direction.toClassName().canonicalName}.navType)")
                    .build()
            )
            .build().writeTo(codeGenerator, Dependencies(false))


        this.addCode("(typeMap = ${directionName}.navType )")
        return this
    }

    private fun FunSpec.Builder.buildScreenRegistry(
        annotation: KSAnnotation,
        directionName: String
    ): FunSpec.Builder {
        val screenRegistry =
            annotation.arguments.first { it.name?.getShortName() == ScreenRegistry::animation.name }.value

        if (screenRegistry !is ArrayList<*>) {

            throw Exception("Missed type of animation in ${ScreenRegistry::class.qualifiedName}")
        }
        if (screenRegistry.isEmpty()) {
            this.addCode("%M<$directionName>", baseComposableRegistrator)
            return this
        }

        if (screenRegistry.size > 1) {
            throw Exception("Only one custom animation allowed")
        }

        val ksType = (screenRegistry.first() as KSType)
        val classDeclaration = ksType.declaration as KSClassDeclaration

        val memberName = MemberName(
            ksType.toClassName(),
            classDeclaration.getAllFunctions().first().simpleName.getShortName()
        )

        this.addCode("%M<$directionName>", memberName)
        return this
    }

    private fun isDataClass(type: KSType): Boolean {
        val declaration = type.declaration
        return declaration is KSClassDeclaration && declaration.modifiers.contains(Modifier.DATA)
    }

    private fun getReturnNavTypeStatement(kClass: KSType): String {
        val name = kClass.toClassName().canonicalName
        return "${typeOfMemberName.canonicalName}<${name}>() to ${serializableTypeMemberName.canonicalName}<${name}>(),"
    }
}

package pl.nepapp.ksp.processor

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
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.asClassName
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

class NavigationProcessorProcessor(
    private val codeGenerator: CodeGenerator,
) : SymbolProcessor {
    private val composableAnnotationClassName = ClassName("androidx.compose.runtime", "Composable")
    private val rememberNavControllerMemberName = MemberName("androidx.navigation.compose", "rememberNavController")
    private val navHostControllerClassName = ClassName("androidx.navigation", "NavHostController")
    private val typeOfMemberName: MemberName = MemberName("kotlin.reflect", "typeOf")
    private val serializableTypeMemberName = MemberName("pl.nepapp.kspproject", "serializableType")

    private val navTypeClassName = ClassName("androidx.navigation", "NavType")
    private val kTypeClassName = ClassName("kotlin.reflect", "KType")
    private val mapType = Map::class.asClassName().parameterizedBy(kTypeClassName, navTypeClassName.parameterizedBy(STAR).copy(annotations = listOf(AnnotationSpec.builder(JvmSuppressWildcards::class).build())))

    private val directionClassName = ClassName("pl.nepapp.kspproject", "Direction")  // Tu wrzucasz package swojego direction interface
    private val baseNavHostClassName = ClassName("pl.nepapp.kspproject", "BaseNavHost")  // Tu wrzucasz package BaseNavHosta
    private val baseComposableRegistrator = MemberName("pl.nepapp.kspproject", "registerBaseComposable")  // Tu wrzucasz i name swojej głównej nawigacji compose
    private val navigationSavedStateHandleClassName = ClassName("pl.nepapp.kspproject", "NavigationSavedStateHandle")
    private val nameOfGeneratedFile = "NavigationGraph" // to jak ma sie nazywac wygenerowany plik


    override fun process(resolver: Resolver): List<KSAnnotated> {
        val symbols = resolver.getSymbolsWithAnnotation(ScreenRegistry::class.qualifiedName!!)
            .filterIsInstance<KSFunctionDeclaration>()

        if (!symbols.iterator().hasNext()) return emptyList()
        val packageName = resolver.getAllFiles().first().packageName.asString()

        val fileSpecBuilder =
            FileSpec.builder(packageName, nameOfGeneratedFile)

        fileSpecBuilder.addFunction(generateNavHostFunction(symbols))

        val fileSpec = fileSpecBuilder.build()
        fileSpec.writeTo(codeGenerator, Dependencies(false))

        return emptyList()
    }

    private fun generateNavHostFunction(symbols: Sequence<KSFunctionDeclaration>): FunSpec {
        val funSpecBuilder = FunSpec.builder(nameOfGeneratedFile)
            .addAnnotation(composableAnnotationClassName)
            .addParameter("initialScreen", directionClassName)
            .addParameter(
                ParameterSpec.builder("navController", navHostControllerClassName)
                    .defaultValue("%M()", rememberNavControllerMemberName)
                    .build()
            )
            .addCode("return %T(\n", baseNavHostClassName)
            .addCode("startDestination = initialScreen,\n")
            .addCode("navController = navController\n")
            .addCode(") {\n")

        symbols.forEach { symbol ->
            val annotation = symbol.annotations.first {
                it.annotationType.resolve().toClassName() == ScreenRegistry::class.asClassName()
            }
            val directionKSType = annotation.arguments.first { it.name?.getShortName() == ScreenRegistry::direction.name }.value as KSType
            val directionClassName = directionKSType.toClassName()
            val animationValue = annotation.arguments.first { it.name?.getShortName() == ScreenRegistry::animation.name }.value

            val screenComposableName = requireNotNull(symbol.qualifiedName).asString()

            funSpecBuilder.buildNavHostScreenComposable(
                animationValue = animationValue,
                directionName = directionClassName.canonicalName
            )

            funSpecBuilder.addNavTypesIfNeeded(
                directionKSType = directionKSType,
                directionClassName = directionClassName,
                codeGenerator = codeGenerator
            )

            funSpecBuilder.addCode("{\n")
            funSpecBuilder.addCode("$screenComposableName()\n")
            funSpecBuilder.addCode("}\n")
        }

        funSpecBuilder.addCode("}\n")

        return funSpecBuilder.build()
    }

    private fun FunSpec.Builder.addNavTypesIfNeeded(
        directionKSType: KSType,
        directionClassName: ClassName,
        codeGenerator: CodeGenerator
    ): FunSpec.Builder {
        val listOfObjects = mutableListOf<ClassName>()
        val fileName = directionClassName.simpleName + "Extension"

        (directionKSType.declaration as KSClassDeclaration).getAllProperties().forEach { field ->
            val fieldResolver = field.type.resolve()
            val fieldClassName = fieldResolver.toClassName()
            if (isDataClass(fieldResolver) && listOfObjects.none { it.canonicalName == fieldClassName.canonicalName }) {
                listOfObjects.add(fieldClassName)
            }
        }

        if (listOfObjects.isEmpty()) {
            return this
        }
        val mapItems = listOfObjects.joinToString("", transform = {
            val name = it.canonicalName
            "${typeOfMemberName.canonicalName}<${name}>() to ${serializableTypeMemberName.canonicalName}<${name}>(),"
        })

        FileSpec.builder(directionClassName.packageName, fileName)
            .addProperty(
                PropertySpec.builder("navType", mapType)
                    .receiver(directionClassName.nestedClass("Companion"))
                    .getter(
                        FunSpec.getterBuilder()
                            .addStatement("return mapOf ( $mapItems )")
                            .build()
                    )
                    .build()
            )

            .addFunction(
                FunSpec.builder("getDirection")
                    .receiver(directionClassName.nestedClass("Companion"))
                    .addParameter(
                        ParameterSpec.builder(
                            "navigationSavedStateHandle", navigationSavedStateHandleClassName
                        ).build()
                    )
                    .returns(directionClassName)
                    .addStatement("return navigationSavedStateHandle.getDirection(${directionClassName.canonicalName}::class, ${directionClassName.canonicalName}.navType)")
                    .build()
            )
            .build().writeTo(codeGenerator, Dependencies(false))

        this.addCode("(typeMap = ${directionClassName.canonicalName}.navType )")
        return this
    }

    private fun FunSpec.Builder.buildNavHostScreenComposable(
        animationValue: Any?,
        directionName: String
    ): FunSpec.Builder {
        if (animationValue !is ArrayList<*>) {

            throw Exception("Missed type of animation in ${ScreenRegistry::class.qualifiedName}")
        }
        if (animationValue.isEmpty()) {
            this.addCode("%M<$directionName>", baseComposableRegistrator)
            return this
        }

        if (animationValue.size > 1) {
            throw Exception("Only one custom animation allowed")
        }

        val ksType = (animationValue.first() as KSType)
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
}

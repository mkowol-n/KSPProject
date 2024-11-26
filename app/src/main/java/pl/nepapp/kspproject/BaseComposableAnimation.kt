package pl.nepapp.kspproject

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlin.reflect.KType

inline fun <reified T : Direction> NavGraphBuilder.registerBaseComposable(
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    composable<T>(
        enterTransition = { NewEnterSlideInHorizontally },
        exitTransition = { NewExitSlideOutHorizontally },
        popExitTransition = { NewPopExitSlideOutHorizontally },
        popEnterTransition = { NewPopExitSlideInHorizontally },
        content = content,
        typeMap = typeMap
    )
}
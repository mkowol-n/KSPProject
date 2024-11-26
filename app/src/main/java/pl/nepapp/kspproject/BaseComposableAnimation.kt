package pl.nepapp.kspproject

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlin.reflect.KType

interface ComposableScreenRegistry {
    fun <T : Direction> NavGraphBuilder.registerBaseComposable(
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
        content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
    )
}


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

object SomeCustomAnimation {
    inline fun <reified T : Direction> NavGraphBuilder.registerComosable(
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
        noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
    ) {
        composable<T>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popExitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            content = content,
            typeMap = typeMap
        )
    }
}

object SomeCustomAnimation2 {
    inline fun <reified T : Direction> NavGraphBuilder.registerComosable(
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
        noinline content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
    ) {
        composable<T>(
            enterTransition = { fadeIn() },
            exitTransition = { fadeOut() },
            popExitTransition = { fadeOut() },
            popEnterTransition = { fadeIn() },
            content = content,
            typeMap = typeMap
        )
    }
}
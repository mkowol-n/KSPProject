package pl.nepapp.kspproject

import android.annotation.SuppressLint
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavType
import androidx.navigation.internalToRoute
import kotlin.reflect.KClass
import kotlin.reflect.KType

class NavigationSavedStateHandle(
    val savedStateHandle: SavedStateHandle
) {
    @SuppressLint("RestrictedApi")
    fun <T : Direction> getDirection(
        clazz: KClass<T>,
        typeMap: Map<KType, @JvmSuppressWildcards NavType<*>>
    ): T {
        return savedStateHandle.internalToRoute(route = clazz, typeMap = typeMap)
    }
}


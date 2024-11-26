package pl.nepapp.kspproject

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import kotlin.reflect.KType

abstract class Screen {
    @Composable
    abstract fun Content()
    open val typeMap: Map<KType, NavType<Any>>? get() = null
}
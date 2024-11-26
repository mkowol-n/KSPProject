package pl.nepapp.kspproject

import androidx.navigation.NavType
import kotlin.reflect.KType

interface Direction {
    val typeMap: Map<KType, NavType<Any>>?
}
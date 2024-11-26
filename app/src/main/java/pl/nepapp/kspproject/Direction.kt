package pl.nepapp.kspproject

import androidx.navigation.NavType
import kotlin.reflect.KType

interface Direction

interface DirectionTypeMapCompanion {
    val typeMap: Map<KType, NavType<Any>>
}
package pl.nepapp.kspproject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavType
import kotlinx.serialization.Serializable
import pl.nepapp.ksp.annotations.ScreenRegistry
import kotlin.reflect.KType

@Serializable
class FirstScreenDirection: Direction {
    override val typeMap: Map<KType, NavType<Any>> get() = navTypeMap

    companion object {
        private val navTypeMap: Map<KType, NavType<Any>> = mapOf()
    }
}

@ScreenRegistry(FirstScreenDirection::class)
@Composable
fun FirstContent() {
    val navigator = LocalNavigator.current
    Box(modifier = Modifier.clickable {
        navigator?.push(SecondScreenDirection)
    }.background(Color.Gray).fillMaxSize()) {}
}
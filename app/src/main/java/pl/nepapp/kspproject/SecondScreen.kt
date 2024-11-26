package pl.nepapp.kspproject

import androidx.compose.foundation.background
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
object SecondScreenDirection: Direction

@ScreenRegistry(SecondScreenDirection::class)
@Composable
fun SecondScreen() {
    Box(modifier = Modifier.background(Color.Red).fillMaxSize())
}
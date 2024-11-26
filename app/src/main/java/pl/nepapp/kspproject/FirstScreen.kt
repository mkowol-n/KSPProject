package pl.nepapp.kspproject

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import pl.nepapp.ksp.annotations.Screen

@Serializable
object FirstScreenDirection: Direction

@Screen(FirstScreenDirection::class)
@Composable
fun FirstScreen() {
    val navigator = LocalNavigator.current
    Box(modifier = Modifier.clickable {
        navigator?.push(SecondScreenDirection)
    }.background(Color.Gray).fillMaxSize()) {}
}
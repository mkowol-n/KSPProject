package pl.nepapp.kspproject

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable
import pl.nepapp.ksp.annotations.ScreenRegistry

@Serializable
data class Test(
    val test: SomeClass,
    val ayako: SomeClassAyako,
    val ayako2: SomeClassAyako,
) : Direction

@ScreenRegistry(Test::class, animation = [SomeCustomAnimation::class])
@Composable
fun ThirdScreen() {
    Box(
        modifier = Modifier
            .background(Color.Red)
            .fillMaxSize()
    )
}
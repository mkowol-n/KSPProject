package pl.nepapp.kspproject

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.IntOffset

private const val FIVE_PERCENT = 0.05f

private val SlightlyRight = { width: Int -> (width * FIVE_PERCENT).toInt() }
private val SlightlyLeft = { width: Int -> 0 - (width * FIVE_PERCENT).toInt() }

val EnterSlideInHorizontally =
    slideInHorizontally(
        initialOffsetX = { it }, animationSpec = spring(
            stiffness = Spring.StiffnessVeryLow,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        )
    )


val ExitSlideOutHorizontally =
    slideOutHorizontally(
        targetOffsetX = { -it }, animationSpec = spring(
            stiffness = Spring.StiffnessVeryLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
    )


val PopExitSlideOutHorizontally =
    slideOutHorizontally(
        targetOffsetX = { it }, animationSpec = spring(
            stiffness = Spring.StiffnessVeryLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
    )


val PopExitSlideInHorizontally =
    slideInHorizontally(
        initialOffsetX = { -it }, animationSpec = spring(
            stiffness = Spring.StiffnessVeryLow,
            visibilityThreshold = IntOffset.VisibilityThreshold
        )
    )


val NewEnterSlideInHorizontally =
    (slideInHorizontally(tween(), SlightlyRight) + fadeIn())


val NewExitSlideOutHorizontally =
    slideOutHorizontally(tween(), SlightlyLeft) + fadeOut()


val NewPopExitSlideOutHorizontally =
    slideOutHorizontally(tween(), SlightlyRight) + fadeOut()


val NewPopExitSlideInHorizontally =
    slideInHorizontally(
        tween(),
        SlightlyLeft
    ) + fadeIn()

package pl.nepapp.kspproject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.navigation.NavController

class Navigator(private val _parent: Navigator?, private val navController: NavController) {
    private val results = mutableStateMapOf<String, Any?>()
    val parent: Navigator get() = requireNotNull(_parent)

    fun replaceAll(directions: List<Direction>) {
        directions.forEachIndexed { index, direction ->
            if (index == 0) {
                replaceAll(direction)
            } else {
                navController.navigate(direction) {
                    launchSingleTop = true
                }
            }
        }
    }

    fun replaceAll(direction: Direction) {
        navController.navigate(direction) {
            popUpTo(0) {
                inclusive = true
            }
            launchSingleTop = true
        }
    }

     fun replace(directions: List<Direction>) {
        directions.forEachIndexed { index, direction ->
            if (index == 0) {
                navController.navigate(direction) {
                    popUpTo(navController.graph.last().id)
                    launchSingleTop = true
                }
            } else {
                navController.navigate(direction) {
                    launchSingleTop = true
                }

            }
        }
    }

    fun replace(direction: Direction) {
        navController.navigate(direction) {
            popUpTo(navController.graph.last().id)
            launchSingleTop = true
        }
    }

    fun push(directions: List<Direction>) {
        directions.forEach {
            navController.navigate(it) {
                launchSingleTop = true
            }
        }
    }

    fun push(direction: Direction) {
        navController.navigate(direction) {
            launchSingleTop = true
        }
    }

    fun pop() {
        navController.popBackStack()
    }

    fun popWithResult(value: Pair<String, Any?>) {
        results[value.first] = value.second
        navController.popBackStack()
    }

    @Composable
    fun <T> getResult(screenKey: String): State<T?> {
        @Suppress("UNCHECKED_CAST") val result = results[screenKey] as? T
        val resultState = remember(screenKey, result) {
            derivedStateOf {
                results.remove(screenKey)
                result
            }
        }
        return resultState
    }
}
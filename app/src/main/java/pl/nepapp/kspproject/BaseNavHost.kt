package pl.nepapp.kspproject

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController

@Composable
fun BaseNavHost(
    startDestination: Direction,
    navController: NavHostController = rememberNavController(),
    builder: NavGraphBuilder.() -> Unit,
) {
    val parentNavigator = LocalNavigator.current

    CompositionLocalProvider(
        LocalNavigator provides Navigator(_parent = parentNavigator, navController = navController)
    ){
        NavHost(
            startDestination = startDestination,
            navController = navController,
            builder = builder
        )
    }
}
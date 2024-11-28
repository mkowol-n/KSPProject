package pl.nepapp.ksp.annotations

import kotlin.reflect.KClass


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ScreenRegistry(val direction: KClass<out Any>, val animation: Array<KClass<out Any>> = [])
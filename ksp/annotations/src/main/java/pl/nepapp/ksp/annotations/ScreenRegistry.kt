package pl.nepapp.ksp.annotations

import kotlin.reflect.KClass


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class ScreenRegistry(val direction: KClass<out Any>)
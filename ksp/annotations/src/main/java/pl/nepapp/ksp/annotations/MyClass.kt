package pl.nepapp.ksp.annotations

import kotlin.reflect.KClass


@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
annotation class Screen(val direction: KClass<out Any>)
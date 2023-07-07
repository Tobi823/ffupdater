package de.marmaro.krt.ffupdater.network.annotation

import androidx.annotation.Keep

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Keep
annotation class ReturnValueMustBeClosed

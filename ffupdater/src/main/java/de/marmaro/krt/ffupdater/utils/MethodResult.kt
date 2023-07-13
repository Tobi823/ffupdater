package de.marmaro.krt.ffupdater.utils

import androidx.annotation.Keep

@Keep
internal class MethodResult(val success: Boolean) {

    inline fun onSuccess(block: (MethodResult) -> Unit): MethodResult {
        if (success) {
            block(this)
        }
        return this
    }

    inline fun onFailure(block: (MethodResult) -> Unit): MethodResult {
        if (!success) {
            block(this)
        }
        return this
    }

    companion object {
        fun failure(): MethodResult {
            return MethodResult(false)
        }

        fun success(): MethodResult {
            return MethodResult(true)
        }
    }
}
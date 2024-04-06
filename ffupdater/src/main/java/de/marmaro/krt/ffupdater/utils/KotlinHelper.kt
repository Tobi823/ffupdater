package de.marmaro.krt.ffupdater.utils

import android.view.View

// https://stackoverflow.com/a/47280844
inline fun Boolean?.ifTrue(block: Boolean.() -> Unit): Boolean? {
    if (this == true) {
        block()
    }
    return this
}

inline fun Boolean.ifTrue(block: Boolean.() -> Unit): Boolean {
    if (this) {
        block()
    }
    return this
}

// https://stackoverflow.com/a/47280844
inline fun Boolean?.ifFalse(block: Boolean?.() -> Unit): Boolean? {
    if (null == this || !this) {
        block()
    }
    return this
}

inline fun Boolean.ifFalse(block: Boolean?.() -> Unit): Boolean {
    if (!this) {
        block()
    }
    return this
}

//fun Boolean.ifTrueThen(): Boolean? {
//    return if (this) true else null
//}


inline fun <T> View.visibleDuringExecution(block: () -> T): T {
    this.visibility = View.VISIBLE
    try {
        return block()
    } finally {
        this.visibility = View.GONE
    }
}

inline fun <T> View.visibleAfterExecution(block: () -> T): T {
    this.visibility = View.VISIBLE
    try {
        return block()
    } finally {
        this.visibility = View.GONE
    }
}
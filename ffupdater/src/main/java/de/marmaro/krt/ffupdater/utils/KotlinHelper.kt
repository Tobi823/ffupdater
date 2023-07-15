package de.marmaro.krt.ffupdater.utils

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
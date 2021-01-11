package com.spqrta.camera2demo.utility.pure

fun <T : Any?> List<T>.nullIfEmpty(): List<T>? = if (this.isEmpty()) {
    null
} else {
    this
}

fun <T> List<T>?.emptyIfNull(): List<T> {
    return this ?: listOf<T>()
}
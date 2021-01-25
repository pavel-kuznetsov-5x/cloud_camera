package com.spqrta.cloudvideo.utility.pure

fun String?.emptyIfNull(): String {
    return this ?: ""
}

fun String.toBase64(): String {
    return Base64Utils.encode(this)
}

fun String?.nullIfEmpty(): String? = if (this.isNullOrEmpty()) {
    null
} else {
    this
}

fun String?.ifNotNullElse(yes: (String) -> Unit, els: () -> Unit) {
    if (this != null) {
        yes.invoke(this)
    } else {
        els.invoke()
    }
}
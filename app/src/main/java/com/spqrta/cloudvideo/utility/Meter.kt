package com.spqrta.cloudvideo.utility

class Meter(
    private val tag: String = "",
    private val showThread: Boolean = false,
    private val disabled: Boolean = false
) {

    private val startTime: Long = System.currentTimeMillis()
    private var lastLogTime: Long = startTime

    init {
        Logg.d("$tag start")
    }

    fun log(message: String? = null) {
        if (!disabled) {
            var res = "$tag ${System.currentTimeMillis() - lastLogTime}"
            if (showThread) {
                res += " ${Thread.currentThread().name}"
            }
            if (message != null) {
                res += " $message"
            }
            Logg.d(res)
            lastLogTime = System.currentTimeMillis()
        }
    }

    fun logFromStart(message: String? = null) {
        if (!disabled) {
            var res = "$tag from start: ${System.currentTimeMillis() - startTime}"
            if (showThread) {
                res += " ${Thread.currentThread().name}"
            }
            if (message != null) {
                res += " $message"
            }
            Logg.d(res)
        }
    }
}
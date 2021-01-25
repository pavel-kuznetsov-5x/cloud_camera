package com.spqrta.cloudvideo.utility

import android.util.Log

object Logg {

    private val logEnabled = CustomApplication.appConfig.debugMode
//    private val logEnabled = true

    var externalLogConsumer: LogConsumer? = null

    const val TAG = "cutag"

    fun v(obj: Any?) {
        if(logEnabled) {
            Log.v(TAG, obj.toString())
            externalLogConsumer?.log(obj.toString(), LogConsumer.Channel.V)
        }
    }

    fun d(obj: Any?) {
        if(logEnabled) {
            Log.d(TAG, obj.toString())
            externalLogConsumer?.log(obj.toString(), LogConsumer.Channel.D)
        }
    }

    fun e(obj: Any) {
        if(logEnabled) {
            Log.e(TAG, obj.toString())
            externalLogConsumer?.log(obj.toString(), LogConsumer.Channel.E)
        }
    }

    fun i(obj: Any) {
        if(logEnabled) {
            Log.i(TAG, obj.toString())
            externalLogConsumer?.log(obj.toString(), LogConsumer.Channel.E)
        }
    }

    fun logListMultiline(list: List<*>) {
        d("\n\n"+list.joinToString("\n"))
    }

    fun thread(tag: String = "") {
        d("$tag - ${Thread.currentThread().name}")
    }

    interface LogConsumer {
        fun log(s: String, channel: Channel = Channel.V)

        enum class Channel {
            V, D, E
        }
    }

}
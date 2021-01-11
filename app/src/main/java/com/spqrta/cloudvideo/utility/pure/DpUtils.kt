package com.spqrta.camera2demo.utility.pure

import android.content.Context
import com.spqrta.camera2demo.utility.CustomApplication

object DpUtils {
    fun dpToPx(dp: Int, context: Context? = null): Float {
        return dpToPx(dp.toFloat(), context)
    }

    fun dpToPx(dp: Float, context: Context? = null): Float {
        return dp * (context ?: CustomApplication.context).resources.displayMetrics.density
    }

    fun pxToDp(px: Float, context: Context? = null): Float {
        return px / (context ?: CustomApplication.context).resources.displayMetrics.density
    }
}

fun Int.dpToPx(): Float {
    return DpUtils.dpToPx(this)
}

fun Float.dpToPx(): Float {
    return DpUtils.dpToPx(this)
}
package com.spqrta.camera2demo.utility

import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

class Toaster {
    companion object {
        fun showException(throwable: Throwable, length: Int = Toast.LENGTH_SHORT) {
            show(
                "${throwable::class.java.simpleName}, ${throwable.message ?: "no message"}",
                length
            )
        }

        fun show(
            text: String,
            length: Int = Toast.LENGTH_SHORT,
            @ColorInt backgroundColor: Int? = null,
            @ColorInt fontColor: Int? = null,
            @DrawableRes backgroundResource: Int? = null,
        ) {
            val toast = Toast.makeText(CustomApplication.context, text, length)
            backgroundColor?.let {
                toast.view!!.setBackgroundColor(it)
            }
            backgroundResource?.let {
                toast.view!!.setBackgroundResource(it)
            }
            fontColor?.let {
                ((toast.view as ViewGroup).getChildAt(0) as TextView).setTextColor(it)
            }
            toast.show()
        }

        fun showResource(
            @StringRes res: Int,
            length: Int = Toast.LENGTH_SHORT,
            @ColorInt backgroundColor: Int? = null,
            @ColorInt fontColor: Int? = null,
            @DrawableRes backgroundResource: Int? = null,
        ) {
            show(
                CustomApplication.context.resources.getString(res),
                length,
                backgroundColor,
                fontColor,
                backgroundResource
            )
        }

        fun showWithDebug(
            @StringRes res: Int,
            debugText: String,
            length: Int = Toast.LENGTH_SHORT
        ) {
            if (CustomApplication.appConfig.releaseMode) {
                showResource(res, length)
            } else {
                show(debugText, length)
            }
        }

        fun showWithDebug(
            @StringRes res: Int,
            throwable: Throwable,
            length: Int = Toast.LENGTH_SHORT
        ) {
            if (CustomApplication.appConfig.releaseMode) {
                showResource(res, length)
            } else {
                showException(throwable, length)
            }
        }
    }
}
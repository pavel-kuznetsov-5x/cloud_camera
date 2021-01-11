package com.spqrta.camera2demo.utility.pure

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.spqrta.camera2demo.utility.CustomApplication

object ResUtils {

    fun getString(@StringRes res: Int): String {
        return CustomApplication.context.getString(res)
    }

    fun getColor(@ColorRes res: Int): Int {
        return CustomApplication.context.resources.getColor(res)
    }

}
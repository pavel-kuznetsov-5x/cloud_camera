package com.spqrta.camera2demo.utility.pure

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Build
import android.util.DisplayMetrics
import android.util.Size


@SuppressLint("DefaultLocale")
object DeviceInfoUtils {


    fun getModel(): String {
        return Build.MODEL.toUpperCase()
    }

    fun getManufacturer(): String {
        return Build.MANUFACTURER.toUpperCase()
    }

    fun getScreenSize(activity: Activity): Size {
        val displayMetrics = DisplayMetrics()
        activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
        return Size(displayMetrics.widthPixels, displayMetrics.heightPixels)
    }

    fun getModelAndManufacturer(): String {
        return "${getManufacturer()} ### ${getModel()}"
    }

    const val SAMSUNG_A20_A205 = "SAMSUNG ### SM-A205FN"
    const val SAMSUNG_A20_A207 = "SAMSUNG ### SM-A207F"
    const val HUAWEI_ELE_L29 = "HUAWEI ### ELE-L29"

}
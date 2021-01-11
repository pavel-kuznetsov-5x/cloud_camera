package com.spqrta.camera2demo.utility.utils

import com.spqrta.camera2demo.utility.pure.DeviceInfoUtils.HUAWEI_ELE_L29
import com.spqrta.camera2demo.utility.pure.DeviceInfoUtils.SAMSUNG_A20_A207

object PreviewAspectRatio {

    fun getForSurface480to640(device: String, frontFacing: Boolean): Float {
        // w/h
        return if(frontFacing) {
            when(device) {
                SAMSUNG_A20_A207 -> ratio640to480
                HUAWEI_ELE_L29 -> ratioSquare
                else -> ratio480to640
            }
        } else {
            when(device) {
                SAMSUNG_A20_A207 -> ratio640to480
                HUAWEI_ELE_L29 -> ratioSquare
                else -> ratio480to640
            }
        }
    }

    const val ratio480to640 = 0.75F
    const val ratio640to480 = 1.33333333F
    const val ratio1080to1920 = 0.5625F
    const val ratioSquare = 1F

}
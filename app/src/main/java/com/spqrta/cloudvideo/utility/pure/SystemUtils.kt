package com.spqrta.cloudvideo.utility.pure

import android.content.ContentResolver
import android.provider.Settings
import android.provider.Settings.SettingNotFoundException

class SystemUtils(private val resolver: ContentResolver) {
    private var autoBrightness = false
    private var brightnessValue = 0

    fun setMaxBrightness() {
        if (autoBrightness) Settings.System.putInt(
            resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
            Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
        )
        Settings.System.putInt(
            resolver,
            Settings.System.SCREEN_BRIGHTNESS,
            255
        )
    }

    fun revertDefault() {
        if (autoBrightness) {
            Settings.System.putInt(
                resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            )
        } else {
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS,
                brightnessValue
            )
        }
    }

    init {
        try {
            autoBrightness = (Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE
            )
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
            if (!autoBrightness) brightnessValue = Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS
            )
        } catch (e: SettingNotFoundException) {
            try {
                brightnessValue = Settings.System.getInt(
                    resolver,
                    Settings.System.SCREEN_BRIGHTNESS
                )
            } catch (e1: SettingNotFoundException) {
                e1.printStackTrace()
            }
            e.printStackTrace()
        }
    }
}
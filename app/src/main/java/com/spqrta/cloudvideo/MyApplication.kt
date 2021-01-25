package com.spqrta.cloudvideo
//todo packages

import com.spqrta.cloudvideo.utility.CustomApplication
import com.spqrta.cloudvideo.utility.pure.FileUtils
import java.io.File

class MyApplication : CustomApplication() {
    override fun createAppConfig(): AppConfig = if (!BuildConfig.DEBUG) {
        AppConfig()
    } else {
        AppConfig(debugMode = true)
    }

    companion object {
        val VIDEOS_FOLDER by lazy {
            FileUtils.ensureFolderExists(
                File(
                    context.externalCacheDir!!,
                    "videos/"
                )
            )
        }
    }


}
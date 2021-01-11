package com.spqrta.cloudvideo
//todo packages

import com.spqrta.camera2demo.utility.CustomApplication
import com.spqrta.camera2demo.utility.pure.FileUtils
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
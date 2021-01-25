package com.spqrta.cloudvideo.utility

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

open class CustomApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        context = applicationContext

        appConfig = createAppConfig()

        if (appConfig.mockMode) {
            Toaster.show("Mock mode!")
        }
    }

    open fun createAppConfig() =
        AppConfig()

    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var context: Context

        lateinit var appConfig: AppConfig

        fun analytics() =
            analytics

        private var analytics: Analytics = object : Analytics() {
            override fun logExceptionToAnalytics(e: Throwable, text: String?) {}
        }

        fun injectAnalytics(analytics: Analytics) {
            Companion.analytics = analytics
        }
    }

    data class AppConfig(
        val debugMode: Boolean = false,
        var stagingMode: Boolean = false,
        var productionMode: Boolean = false,
        var debugNavigation: Boolean = false,
        val mockMode: Boolean = false,
        val sendErrorsToAnalyticsInDebugMode: Boolean = false,
        val throwInAnalytics: Boolean = false,
        val disableDataPrefill: Boolean = false
    ) {
        val releaseMode = !debugMode
        val prefillData = debugMode && !disableDataPrefill
        val notMockMode = releaseMode || (debugMode && !mockMode)
    }
}
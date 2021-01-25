package com.spqrta.cloudvideo.utility


abstract class Analytics {

    open fun logException(e: Throwable, text: String? = null) {
        if (CustomApplication.appConfig.throwInAnalytics) {
            throw e
        }
//        val logText = when(e) {
//            is BackendException -> {
//                "$e $text"
//            }
//            is HttpException -> {
//                e.response()?.raw()?.request()?.url().toString() + "\n\t" + text
//            }
//            else -> text
//        }
//
//        if (!e.isNetworkError() && e !is NetworkError) {
//            if (CustomApplication.appConfig.releaseMode
//                || CustomApplication.appConfig.sendErrorsToAnalyticsInDebugMode
//            ) {
//                logExceptionToAnalytics(e, logText)
//            }
//        }
    }

    abstract fun logExceptionToAnalytics(e: Throwable, text: String? = null)
}
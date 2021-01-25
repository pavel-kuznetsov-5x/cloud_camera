package com.spqrta.cloudvideo.base.mixins

import com.spqrta.cloudvideo.utility.CustomApplication
import com.spqrta.cloudvideo.base.display.Payload
import com.spqrta.cloudvideo.base.display.State
import com.spqrta.cloudvideo.utility.Toaster
import com.spqrta.cloudvideo.base.display.JustError

interface ErrorToastMixin {

    fun applyErrorToastMixin(state: State<Payload>) {
        if (state is JustError) {
            CustomApplication.analytics().logException(state.exception)
            applyErrorToastMixin(state.exception)
        }
    }

    fun applyErrorToastMixin(exception: Throwable) {
        exception.printStackTrace()
        Toaster.show(exception.message ?: "Unknown error")
    }

}
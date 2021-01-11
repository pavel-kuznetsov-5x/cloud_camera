package com.spqrta.camera2demo.base.mixins

import com.spqrta.camera2demo.utility.CustomApplication
import com.spqrta.camera2demo.base.display.Payload
import com.spqrta.camera2demo.base.display.State
import com.spqrta.camera2demo.utility.Toaster
import com.spqrta.camera2demo.base.display.JustError

interface ErrorToastMixin {

    fun applyErrorToastMixin(state: State<Payload>) {
        if (state is JustError) {
            CustomApplication.analytics().logException(state.exception)
            applyErrorToastMixin(state.exception)
        }
    }

    fun applyErrorToastMixin(exception: Throwable) {
        Toaster.show(exception.message ?: "Unknown error")
    }

}
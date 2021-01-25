package com.spqrta.cloudvideo.base.mixins

import android.view.View
import com.spqrta.cloudvideo.base.display.JustLoading
import com.spqrta.cloudvideo.base.display.Payload
import com.spqrta.cloudvideo.base.display.State
import com.spqrta.cloudvideo.utility.pure.hide
import com.spqrta.cloudvideo.utility.pure.show

interface ProgressbarMixin {

    fun applyProgressbarMixin(state: State<Payload>, progressbarView: View) {
        if (state is JustLoading) {
            progressbarView.show()
        } else {
            progressbarView.hide()
        }
    }

}
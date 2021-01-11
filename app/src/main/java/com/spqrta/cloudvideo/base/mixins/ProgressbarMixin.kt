package com.spqrta.camera2demo.base.mixins

import android.view.View
import com.spqrta.camera2demo.base.display.JustLoading
import com.spqrta.camera2demo.base.display.Payload
import com.spqrta.camera2demo.base.display.State
import com.spqrta.camera2demo.utility.pure.hide
import com.spqrta.camera2demo.utility.pure.show

interface ProgressbarMixin {

    fun applyProgressbarMixin(state: State<Payload>, progressbarView: View) {
        if (state is JustLoading) {
            progressbarView.show()
        } else {
            progressbarView.hide()
        }
    }

}
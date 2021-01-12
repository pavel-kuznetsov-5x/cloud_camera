package com.spqrta.reusables.base.network

import com.spqrta.camera2demo.utility.CustomApplication
import com.spqrta.cloudvideo.R

class NetworkError: Throwable() {

    override val message: String?
        get() = CustomApplication.context.getString(R.string.network_error)
}
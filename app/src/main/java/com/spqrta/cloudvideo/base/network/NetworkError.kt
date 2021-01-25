package com.spqrta.cloudvideo.base.network

import com.spqrta.cloudvideo.utility.CustomApplication
import com.spqrta.cloudvideo.R

class NetworkError: Throwable() {

    override val message: String?
        get() = CustomApplication.context.getString(R.string.network_error)
}
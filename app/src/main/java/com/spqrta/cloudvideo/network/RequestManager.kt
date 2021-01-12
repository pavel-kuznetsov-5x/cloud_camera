package com.spqrta.cloudvideo.network

import com.spqrta.reusables.base.network.BaseRequestManager

object RequestManager: BaseRequestManager() {

    override fun getBaseUrl(): String = "https://www.googleapis.com/"

    val api: Api by lazy { retrofit.create(Api::class.java) }
}
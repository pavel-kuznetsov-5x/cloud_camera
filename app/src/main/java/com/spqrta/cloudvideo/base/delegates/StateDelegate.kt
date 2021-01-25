package com.spqrta.cloudvideo.base.delegates

import android.os.Bundle
import com.spqrta.cloudvideo.base.display.BaseFragment
import com.spqrta.cloudvideo.utility.CustomApplication
import com.spqrta.cloudvideo.MainActivity

open class StateDelegate<T>(
    fragment: BaseFragment<MainActivity>
) : BaseFragment.FragmentDelegate<MainActivity>(fragment) {

    private val key = hashCode().toString()

    var state: T? = null

    override fun onSaveState(bundle: Bundle) {
        state?.let { state ->
            when (state) {
                is Int -> bundle.putInt(key, (state as Int))
                is Boolean -> bundle.putBoolean(key, (state as Boolean))
                else -> if (CustomApplication.appConfig.debugMode) {
                    throw IllegalStateException("state of unknown type")
                }
            }
        }
    }

    override fun onLoadState(bundle: Bundle) {
        try {
            when (state) {
                is Int? -> state = bundle.getInt(key) as T
                is Boolean? -> state = bundle.getBoolean(key) as T
                else -> if (CustomApplication.appConfig.debugMode) {
                    throw IllegalStateException("state of unknown type")
                }
            }
        } catch (e: Exception) {
            CustomApplication.analytics().logException(e)
        }
    }
}
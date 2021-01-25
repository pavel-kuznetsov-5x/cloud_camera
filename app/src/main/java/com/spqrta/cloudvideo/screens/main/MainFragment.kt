package com.spqrta.cloudvideo.screens.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.spqrta.cloudvideo.base.display.BaseFragment
import com.spqrta.cloudvideo.MainActivity
import com.spqrta.cloudvideo.R

class MainFragment : BaseFragment<MainActivity>() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }


}
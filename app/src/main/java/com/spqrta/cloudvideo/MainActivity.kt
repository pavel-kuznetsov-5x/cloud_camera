package com.spqrta.cloudvideo

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.spqrta.cloudvideo.base.display.NavActivity
import com.spqrta.cloudvideo.base.mixins.ErrorToastMixin
import com.spqrta.cloudvideo.repository.DriveRepository
import java.lang.IllegalArgumentException

class MainActivity : NavActivity(), ErrorToastMixin {

    override val layoutRes = R.layout.activity_nav

    private var driveServiceReady: Boolean = false

    val connection = MyConnection()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DriveRepository.initForResult(this)
    }

    private fun onDriveServiceReady() {
        driveServiceReady = true
        val serviceIntent = Intent(this, SyncService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

        bindService()
    }

    override fun onStart() {
        super.onStart()
        if (driveServiceReady) {
            bindService()
        }
    }


    override fun onStop() {
        super.onStop()
        try {
            unbindService(connection)
            connection.service?.stopForeground(true)
        } catch (e: IllegalArgumentException) {
            if(e.message?.contains("Service not registered") == false) {
                throw e
            }
        }
        connection.bound = false
    }

    private fun bindService() {
        bindService(Intent(this, SyncService::class.java), connection, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        DriveRepository.onInitResult(this, requestCode, resultCode, resultData).subscribeManaged({
            onDriveServiceReady()
        }, {
            applyErrorToastMixin(it)
        })
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    inner class MyConnection : ServiceConnection {
        var bound: Boolean = false
        var service: SyncService? = null

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            this.service = (service as SyncService.MyBinder).service
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            service = null
            bound = false
        }
    }




}
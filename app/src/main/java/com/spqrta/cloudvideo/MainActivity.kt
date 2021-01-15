package com.spqrta.cloudvideo

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.spqrta.camera2demo.base.display.NavActivity
import com.spqrta.camera2demo.base.mixins.ErrorToastMixin
import com.spqrta.camera2demo.utility.gms.toSingle
import com.spqrta.cloudvideo.repository.DriveRepository
import io.reactivex.Single
import java.lang.IllegalArgumentException

//todo app name
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
            e.printStackTrace()
            //todo
        // java.lang.IllegalArgumentException: Service not registered: com.spqrta.cloudvideo.MainActivity$MyConnection@c35fc10
        }
        connection.bound = false
    }

    //todo empty videos on start
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
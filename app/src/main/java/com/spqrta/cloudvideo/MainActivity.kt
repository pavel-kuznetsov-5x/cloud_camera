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


class MainActivity : NavActivity(), ErrorToastMixin {




    override val layoutRes = R.layout.activity_nav

    private lateinit var service: SyncService
    private var bound: Boolean = false
    private var driveServiceReady: Boolean = false

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder = service as SyncService.MyBinder
            this@MainActivity.service = binder.service
            bound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            bound = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        DriveRepository.initForResult(this)
    }

    private fun onDriveServiceReady() {
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
        unbindService(connection)
        bound = false
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



//    private fun createFile() {
//        if (driveServiceHelper != null) {
//            driveServiceHelper!!.createFile()
//                    .addOnSuccessListener { fileId -> readFile(fileId) }
//                    .addOnFailureListener { exception ->
//                        throw exception
//                    }
//        }
//    }

//    private fun readFile(fileId: String) {
//        if (driveServiceHelper != null) {
//            driveServiceHelper!!.readFile(fileId)
//                    .addOnSuccessListener { nameAndContent ->
//                        val name = nameAndContent.first
//                        val content = nameAndContent.second
////                        mFileTitleEditText.setText(name)
////                        mDocContentEditText.setText(content)
////                        setReadWriteMode(fileId)
//                    }
//                    .addOnFailureListener { exception ->
//                        throw exception
//                    }
//        }
//    }

    private fun saveFile() {
//        if (mDriveServiceHelper != null && mOpenFileId != null) {
//            Log.d(MainActivity.TAG, "Saving $mOpenFileId")
//            val fileName: String = mFileTitleEditText.getText().toString()
//            val fileContent: String = mDocContentEditText.getText().toString()
//            mDriveServiceHelper!!.saveFile(mOpenFileId, fileName, fileContent)
//                    .addOnFailureListener { exception -> Log.e(MainActivity.TAG, "Unable to save file via REST.", exception) }
//        }
    }

}
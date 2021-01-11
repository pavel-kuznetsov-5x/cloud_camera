package com.spqrta.cloudvideo.repository

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.spqrta.camera2demo.utility.gms.toSingle
import com.spqrta.camera2demo.utility.pure.FileUtils
import com.spqrta.camera2demo.utility.pure.FileUtils.size
import com.spqrta.camera2demo.utility.pure.Stub
import com.spqrta.camera2demo.utility.utils.RxUtils
import com.spqrta.cloudvideo.DriveServiceHelper
import com.spqrta.cloudvideo.MainActivity
import com.spqrta.cloudvideo.MyApplication
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.SingleSubject
import java.io.File

object DriveRepository {

    private const val REQUEST_CODE_SIGN_IN = 1

    private lateinit var driveServiceHelper: DriveServiceHelper

    fun initForResult(activity: MainActivity) {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(Scope(DriveScopes.DRIVE_FILE))
                .build()
        val client = GoogleSignIn.getClient(activity, signInOptions)

        activity.startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    fun onInitResult(activity: MainActivity, requestCode: Int, resultCode: Int, resultData: Intent?): Single<Stub> {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> if (resultCode == AppCompatActivity.RESULT_OK && resultData != null) {
                return GoogleSignIn.getSignedInAccountFromIntent(resultData).toSingle().doOnSuccess { googleAccount ->
                    val credential = GoogleAccountCredential.usingOAuth2(
                            activity, setOf(DriveScopes.DRIVE_FILE))
                    credential.selectedAccount = googleAccount.account
                    val googleDriveService = Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            GsonFactory(),
                            credential)
                            .setApplicationName("Drive API Migration")
                            .build()

                    driveServiceHelper = DriveServiceHelper(googleDriveService)
                }.map { Stub }
            }
        }
        return Single.never()
    }

    fun getDriveVideos(): Single<FileList> {
        return driveServiceHelper.queryFiles().toSingle()
    }
}
package com.spqrta.cloudvideo.repository

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.FileList
import com.spqrta.camera2demo.utility.Logg
import com.spqrta.camera2demo.utility.gms.toSingle
import com.spqrta.camera2demo.utility.gms.toSingleNullable
import com.spqrta.camera2demo.utility.pure.Stub
import com.spqrta.camera2demo.utility.utils.applySchedulers
import com.spqrta.cloudvideo.DriveServiceHelper
import com.spqrta.cloudvideo.MainActivity
import com.spqrta.cloudvideo.network.Api
import com.spqrta.cloudvideo.network.RequestManager
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File


object DriveRepository {

    private const val REQUEST_CODE_SIGN_IN = 1

    private lateinit var driveServiceHelper: DriveServiceHelper

    private lateinit var videosFolderId: String

    //todo secure
    private lateinit var token: String

    fun initForResult(activity: MainActivity) {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_FILE))
            .build()
        val client = GoogleSignIn.getClient(activity, signInOptions)

        activity.startActivityForResult(client.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    fun onInitResult(
        activity: MainActivity,
        requestCode: Int,
        resultCode: Int,
        resultData: Intent?
    ): Single<Stub> {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> if (resultCode == AppCompatActivity.RESULT_OK && resultData != null) {
                return GoogleSignIn.getSignedInAccountFromIntent(resultData).toSingle()
                    .observeOn(Schedulers.io())
                    .doOnSuccess { googleAccount ->
                        Logg.d(Thread.currentThread().name)
                        val credential = GoogleAccountCredential.usingOAuth2(
                            activity, setOf(DriveScopes.DRIVE_FILE)
                        )
                        credential.selectedAccount = googleAccount.account
                        token = credential.token
                        val googleDriveService = Drive.Builder(
                            AndroidHttp.newCompatibleTransport(),
                            GsonFactory(),
                            credential
                        )
                            .setApplicationName("Drive API Migration")
                            .build()

                        driveServiceHelper = DriveServiceHelper(googleDriveService)
                    }
                    .flatMap {
                        ensureFolderExists(File("CloudCamera"))
                            .subscribeOn(Schedulers.io())
                    }
                    .doOnSuccess {
                        videosFolderId = it
                    }
                    .applySchedulers()
                    .map { Stub }
            }
        }
        return Single.never()
    }

    //todo pagination
    fun getDriveVideos(): Single<FileList> {
        return driveServiceHelper.queryFiles(videosFolderId).toSingle()
    }

    fun saveVideo(file: File): Single<Stub> {
//        return driveServiceHelper.createFile(file, videosFolderId).toSingle().map { Stub }
        return RequestManager.api
            .initPartialUpload(token = "Bearer ${token}", metadata = Api.Metadata(file.name))
            .applySchedulers()
            .flatMap {
                val fbody = RequestBody.create(MediaType.parse("video/mp4"), file)

                return@flatMap RequestManager.api
                    .uploadFile(
                        uploadId = it.headers()["x-guploader-uploadid"]!!,
//                        metadata = metadata,
                        file = fbody
                    )
                    .applySchedulers()

            }
            .map { Stub }
    }

    fun ensureFolderExists(file: File): Single<String> {
        return driveServiceHelper.searchFolder(file).toSingleNullable()
            .flatMap {
                if (it.isEmpty) {
                    driveServiceHelper.createFolder(file).toSingle()
                } else {
                    Single.just(it.get())
                }
            }
    }


}
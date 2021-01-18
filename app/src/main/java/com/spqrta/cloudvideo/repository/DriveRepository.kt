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
import com.spqrta.camera2demo.utility.pure.FileUtils.size
import com.spqrta.camera2demo.utility.pure.Stub
import com.spqrta.camera2demo.utility.utils.applySchedulers
import com.spqrta.cloudvideo.DriveServiceHelper
import com.spqrta.cloudvideo.MainActivity
import com.spqrta.cloudvideo.network.Api
import com.spqrta.cloudvideo.network.RequestManager
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import java.lang.Thread.sleep
import io.reactivex.*
import okhttp3.Headers
import retrofit2.HttpException
import java.lang.Exception


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
        return driveServiceHelper.createFile(file, videosFolderId).toSingle().map { Stub }
    }

    fun getFileUploadState(file: File) {

    }

    fun initResumableUpload(file: File): Single<ResumableMetadata> {
        return RequestManager.api
                .initPartialUpload(
                        token = "Bearer ${token}",
                        metadata = Api.Metadata(file.name, listOf(videosFolderId))
                )
                .applySchedulers()
                .map {
                    ResumableMetadata.fromHeaders(it.headers())
                }
    }

    fun uploadChunk(
            uploadId: String,
            byteArray: ByteArray,
            offset: Long,
            finalSize: Long? = null,
            edit: Boolean = false
    ): Single<Stub> {
        val fbody = RequestBody.create(MediaType.parse("video/mp4"), byteArray)

        val contentRange = Api.formatContentRange(
                offset, offset + byteArray.size.toLong() - 1, finalSize)

        return if (edit) {
            RequestManager.api
                    .editChunk(
                            contentRange = contentRange, uploadId = uploadId, file = fbody
                    )
        } else {
            RequestManager.api
                    .uploadChunk(
                            contentRange = contentRange, uploadId = uploadId, file = fbody
                    )
        }
                .subscribeOn(Schedulers.io())
                .doOnSuccess {
                    if (!it.isSuccessful && it.code() != 308) {
                        throw Exception("${it.raw().request().headers()["Content-Range"]} | ${it.errorBody()!!.string()}")
                    }
                }
                .map { Stub }
    }

    fun uploadFileBytes(name: String, bytes: ByteArray): Single<Stub> {
        return initResumableUpload(File(name))
                .flatMap {
                    uploadChunk(it.uploadId, bytes, 0, bytes.size.toLong())
                }
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

    data class ResumableMetadata(
            val uploadId: String,
            val location: String,
    ) {
        companion object {
            fun fromHeaders(headers: Headers) = ResumableMetadata(
                    uploadId = headers["x-guploader-uploadid"]!!,
                    location = headers["location"]!!,
            )
        }
    }

}
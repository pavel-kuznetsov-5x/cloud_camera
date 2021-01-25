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
import com.spqrta.cloudvideo.utility.gms.toSingle
import com.spqrta.cloudvideo.utility.gms.toSingleNullable
import com.spqrta.cloudvideo.utility.pure.FileUtils.size
import com.spqrta.cloudvideo.utility.pure.Stub
import com.spqrta.cloudvideo.utility.utils.applySchedulers
import com.spqrta.cloudvideo.DriveApiWrapper
import com.spqrta.cloudvideo.MainActivity
import com.spqrta.cloudvideo.network.Api
import com.spqrta.cloudvideo.network.RequestManager
import com.spqrta.cloudvideo.utility.Logg
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import java.io.File
import io.reactivex.*
import io.reactivex.functions.BiFunction
import okhttp3.Headers
import java.lang.Exception


object DriveRepository {

    private const val REQUEST_CODE_SIGN_IN = 1

    private lateinit var DriveApiWrapper: DriveApiWrapper

    lateinit var videosFolderId: String

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

                        DriveApiWrapper = DriveApiWrapper(googleDriveService)
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
        return DriveApiWrapper.getFiles(videosFolderId).toSingle()
    }

    fun saveVideo(file: File): Single<Stub> {
        return DriveApiWrapper.createFile(file, videosFolderId).toSingle().map { Stub }
    }

    @Suppress("SENSELESS_COMPARISON")
    fun getFileUploadState(file: File): Single<UploadState> {
        val uploadId = DatabaseRepository.getUploadId(file)
        if (uploadId == null) {
            val driveFile =
                DriveApiWrapper.searchFile(file).toSingleNullable().blockingGet().getNullable()
            if (driveFile != null) {
                if (file.size() == driveFile.getSize()) {
                    return Single.just(Completed)
                }
            }
            return Single.just(NotInited)
        } else {
            return RequestManager.api.checkUploadStatus(Api.unknownContentRange(), uploadId)
                .map {
                    return@map when (it.code()) {
                        200, 201 -> Completed
                        308 -> Resumable(
                            uploadId,
                            Resumable.parseRangeHeader(it.headers()) ?: 0
                        )
                        else -> NotInited
                    }
                }
        }
    }

    fun initResumableUpload(file: File, parentId: String? = null): Single<ResumableMetadata> {
        return RequestManager.api
            .initPartialUpload(
                token = "Bearer ${token}",
                metadata = Api.Metadata(file.name, listOf(parentId ?: videosFolderId))
            )
            .applySchedulers()
            .map {
                ResumableMetadata.fromHeaders(it.headers())
            }
            .doOnSuccess {
                DatabaseRepository.saveUploadId(file, it.uploadId)
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
            offset, offset + byteArray.size.toLong() - 1, finalSize
        )

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
                    throw Exception(
                        "${it.raw().request().headers()["Content-Range"]} | ${it.errorBody()!!
                            .string()}"
                    )
                }
            }
            .map { Stub }
    }

    fun uploadFileBytes(name: String, bytes: ByteArray, parentId: String?): Single<Stub> {
        return initResumableUpload(File(name), parentId)
            .flatMap {
                uploadChunk(it.uploadId, bytes, 0, bytes.size.toLong())
            }
    }

    fun ensureFolderExists(file: File, parentId: String? = null): Single<String> {
        return DriveApiWrapper.searchFolder(file, parentId).toSingleNullable()
            .flatMap {
                if (it.isEmpty) {
                    DriveApiWrapper.createFolder(file, parentId).toSingle()
                } else {
                    Single.just(it.get())
                }
            }
    }

    fun removeRecordingFoldersForSyncedVideos(): Single<Stub> {
        return Single.zip(
            DriveApiWrapper.getFiles(videosFolderId).toSingle(),
            DriveApiWrapper.getFolders(videosFolderId).toSingle(),
            BiFunction<FileList, FileList, Stub> { files, folders ->
                check(folders.files.firstOrNull()?.capabilities!!.canAddChildren)
                check(files.files.firstOrNull()?.capabilities!!.canAddChildren.not())
                for (folder in folders.files) {
                    for(f in files.files) {
                        if(f.name == folder.name) {
                            check(folder.capabilities!!.canAddChildren)
                            //todo check size before deletion
                            DriveApiWrapper.deleteFile(folder.id)
                        }
                    }
                }
                Stub
            }
        )
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

    open class UploadState
    object Completed : UploadState()
    object NotInited : UploadState()
    class Resumable(val uploadId: String, val bytesLoaded: Long) : UploadState() {
        companion object {
            fun parseRangeHeader(headers: Headers): Long? {
                return headers.get("Range")?.let {
                    it.replace("bytes=", "").split("-")[1].toLong()
                }
            }
        }
    }

}
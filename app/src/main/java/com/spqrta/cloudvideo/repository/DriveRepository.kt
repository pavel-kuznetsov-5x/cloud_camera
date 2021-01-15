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
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function
import io.reactivex.internal.functions.ObjectHelper
import io.reactivex.internal.operators.single.SingleZipIterable
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File
import java.lang.Thread.sleep
import io.reactivex.*


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
//        return driveServiceHelper.createFile(file, videosFolderId).toSingle().map { Stub }

        Logg.d(file.size())
        val chunk_size = 1000 * 1024
//        val chunks = mutableListOf<Long>()
//        for (i in 0..file.size() / chunk_size) {
//            if (i != file.size() / chunk_size) {
//                chunks.add(chunk_size.toLong())
//            } else {
//                chunks.add(file.size() % chunk_size)
//            }
//        }
        val chunks = listOf(1, 2, 3, 4)

        chunks.forEachIndexed { i, c ->
            Logg.d("${i * chunk_size} ${i * chunk_size + c - 1}")
        }

        return RequestManager.api
                .initPartialUpload(
                        token = "Bearer ${token}",
                        metadata = Api.Metadata(file.name, listOf(videosFolderId))
                )
                .subscribeOn(Schedulers.io())
                .flatMap { resp ->
                    Observable.fromIterable(chunks)
                            //todo group
                            .map { sleep(200) }
                            .flatMap {
                                Logg.thread("Chunks")
                                val fbody = RequestBody.create(MediaType.parse("video/mp4"), file)

                                RequestManager.api
                                        .uploadFile(
                                                uploadId = resp.headers()["x-guploader-uploadid"]!!,
                                                file = fbody
                                        )
                            }
                            .toList()
                            .map {
                                Logg.thread("map")
                                Stub
                            }
                }
                .observeOn(AndroidSchedulers.mainThread())

//
//            .flatMap { resp ->
//
//                    .subscribeOn(AndroidSchedulers.mainThread())
//            }
//            .subscribeOn(AndroidSchedulers.mainThread())
    }

    fun <T, R> z(
            sources: Iterable<SingleSource<out T>?>?,
            zipper: Function<in Array<Any?>?, out R>?
    ): Single<R>? {
        ObjectHelper.requireNonNull(zipper, "zipper is null")
        ObjectHelper.requireNonNull(sources, "sources is null")
        return RxJavaPlugins.onAssembly(SingleZipIterable(sources, zipper))
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
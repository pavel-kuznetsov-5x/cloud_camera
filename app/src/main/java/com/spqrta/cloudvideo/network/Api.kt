package com.spqrta.cloudvideo.network

import com.google.api.services.drive.model.User
import com.spqrta.camera2demo.utility.pure.Stub
import io.reactivex.Single
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*


interface Api {

    @POST("/upload/drive/v3/files")
    fun initPartialUpload(
            @Header("Authorization")  token: String,
            @Query("uploadType") uploadType: String = "resumable",
            @Query("upload_id") uploadId: String? = null,
            @Body metadata: Metadata
    ): Single<Response<Void>>
    class Metadata(
        val name: String
    )

    //todo to reusables
    @PUT("/upload/drive/v3/files")
    fun uploadFile(
            @Query("uploadType") uploadType: String = "resumable",
            @Query("upload_id") uploadId: String,
//            @Part("metadata") metadata: RequestBody?,
            @Body file: RequestBody
    ): Single<Response<Void>>
}
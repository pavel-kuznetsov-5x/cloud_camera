package com.spqrta.cloudvideo.network

import io.reactivex.Single
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
        val name: String,
        val parents: List<String>
    )

    @PUT("/upload/drive/v3/files")
    fun uploadChunk(
            @Header("Content-Range")  contentRange: String,
            @Query("uploadType") uploadType: String = "resumable",
            @Query("upload_id") uploadId: String,
            @Body file: RequestBody
    ): Single<Response<Void>>

    @PATCH("/upload/drive/v3/files")
    fun editChunk(
            @Header("Content-Range")  contentRange: String,
            @Query("uploadType") uploadType: String = "resumable",
            @Query("upload_id") uploadId: String,
            @Body file: RequestBody
    ): Single<Response<Void>>

    @PUT("/upload/drive/v3/files")
    fun checkUploadStatus(
            @Header("Content-Range")  contentRange: String,
            @Query("upload_id") uploadId: String,
            @Query("uploadType") uploadType: String = "resumable",
    ): Single<Response<Void>>

    companion object {
        fun formatContentRange(start: Long, end: Long, total: Long? = null): String {
            if(total != null) {
                return "bytes $start-$end/$total"
            } else {
                return "bytes $start-$end/*"
            }
        }

        fun unknownContentRange(): String {
            return "bytes */*"
        }
    }

}
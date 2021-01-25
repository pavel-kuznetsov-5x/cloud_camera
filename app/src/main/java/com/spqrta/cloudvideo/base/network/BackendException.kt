package com.spqrta.cloudvideo.base.network

import com.spqrta.cloudvideo.utility.pure.AppUtils
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import org.json.JSONArray

open class BackendException(
    val code: Int,
    private val response: Response<*>?,
    private val errorBody: String?
) : RuntimeException() {

    private val jsonBody by lazy {
        try {
            if (errorBody != null) {
                JSONObject(errorBody)
            } else {
                JSONObject("{message:\"No message\"}")
            }
        } catch (e: JSONException) {
            JSONObject("{message:\"Error parsing failed\"}")
        }
    }

    override val message: String?
        get() = jsonBody.getString("message")

    override fun toString(): String {
        return AppUtils.ifReleaseModeElse({
            message ?: "No message"
        }, {
            "${code} ${response?.raw()?.request()?.url()}: ${message}"
        })
    }

//    val multipleErrors: Boolean
//        get() = jsonBody.has("errors")

    companion object {
        fun parseErrors(errors: JSONArray): List<String> {
//            jsonBody.getJSONObject("errors").keys().asSequence().map {
//                jsonBody.getJSONObject("errors").getJSONArray(it).get(0)
//            }.joinToString("\n")
            return listOf()
        }
    }
}
package com.spqrta.reusables.base.network

import com.spqrta.camera2demo.utility.CustomApplication
import com.spqrta.camera2demo.utility.Logg
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory


@Suppress("LeakingThis")
abstract class BaseRequestManager {

    protected lateinit var retrofit: Retrofit

    init {
        buildRetrofit()
    }

    protected fun buildRetrofit() {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            if (CustomApplication.appConfig.debugMode) {
                Logg.v(message)
//                if(message.length < 256) {
//                    if(message.contains("-->") or message.contains("<--")) {
//                        Logg.d(message)
//                    } else {
//                        Logg.v(message)
//                    }
//                } else {
//                    try {
//                        Logg.v(JSONObject(message).toString(4))
//                    } catch (e: JSONException) {
//                        Logg.v(message)
//                    }
//                }
            }
        }
        loggingInterceptor.level = HttpLoggingInterceptor.Level.BODY

        val clientBuilder = OkHttpClient.Builder()

        buildClient(clientBuilder)
        if (CustomApplication.appConfig.debugMode) {
            clientBuilder.addInterceptor(loggingInterceptor)
        }

        val client = clientBuilder
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(getBaseUrl())
            .client(client)
//            .addConverterFactory(NullOnEmptyConverterFactory())
            .addConverterFactory(GsonConverterFactory.create())
//            .addConverterFactory(ScalarsConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
    }

    abstract fun getBaseUrl(): String

    open fun buildClient(builder: OkHttpClient.Builder) {

    }


}
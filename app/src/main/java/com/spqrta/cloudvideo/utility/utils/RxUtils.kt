package com.spqrta.cloudvideo.utility.utils

import android.view.View
import com.spqrta.cloudvideo.utility.pure.Stub
import com.spqrta.cloudvideo.utility.pure.hide
import com.spqrta.cloudvideo.utility.pure.show
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object RxUtils {
    fun initialSingle(): Single<Stub> {
        return Single.just(Stub)
    }

    fun runInBackground(runnable: () -> Unit): Single<Stub> {
        return Single.fromCallable { runnable.invoke() }
            .map { Stub }
            .applySchedulers()
    }
}

fun <T> Observable<T>.applySchedulers(): Observable<T> {
    return subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
}


fun <T> Single<T>.applySchedulers(): Single<T> {
    return subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
}


fun <T> Single<T>.disableClicksWhenLoading(vararg views: View): Single<T> {
    return doOnSubscribe {
        views.forEach {
            it.isClickable = false
        }
    }.doOnEvent { t1, t2 ->
        views.forEach {
            it.isClickable = true
        }
    }
}

open class RxResult<T>(val value: T)
open class RxStringResult(value: String) : RxResult<String>(value) {
    override fun toString() = value
}

class AccessToken(value: String) : RxStringResult(value)

fun <T> Single<T>.delayExecution(milliseconds: Int): Single<T> {
    return delay(milliseconds.toLong(), TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
}

fun <T> Single<T>.attachProgressbar(progressBar: View): Single<T> {
    return doOnSubscribe {
        progressBar.show()
    }.doOnEvent { _, _ ->
        progressBar.hide()
    }
}

fun <T> Single<T>.delayExecution(milliseconds: Long): Single<T> {
    return delay(milliseconds, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
}
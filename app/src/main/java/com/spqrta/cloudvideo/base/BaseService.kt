package com.spqrta.cloudvideo.base

import android.annotation.SuppressLint
import android.app.Service
import android.content.pm.ActivityInfo
import android.os.Bundle
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseService: Service() {

    private lateinit var compositeDisposable: CompositeDisposable

    override fun onCreate() {
        super.onCreate()
        compositeDisposable = CompositeDisposable()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    fun <T> Single<T>.subscribeManaged(onSuccess: (T) -> Unit): Disposable {
        val disposable = subscribe(onSuccess)
        compositeDisposable.add(disposable)
        return disposable
    }

    fun <T> Single<T>.subscribeManaged(
            onSuccess: (T) -> Unit,
            onError: (Throwable) -> Unit
    ): Disposable {
        val disposable = subscribe(onSuccess, onError)
        compositeDisposable.add(disposable)
        return disposable
    }

    fun <T> Observable<T>.subscribeManaged(onSuccess: (T) -> Unit): Disposable {
        val disposable = subscribe(onSuccess)
        compositeDisposable.add(disposable)
        return disposable
    }

    fun <T> Observable<T>.subscribeManaged(
            onSuccess: (T) -> Unit,
            onError: (Throwable) -> Unit
    ): Disposable {
        val disposable = subscribe(onSuccess, onError)
        compositeDisposable.add(disposable)
        return disposable
    }

}
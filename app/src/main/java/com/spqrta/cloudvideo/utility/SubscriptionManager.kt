package com.spqrta.cloudvideo.utility

import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

open class SubscriptionManager {

    private var compositeDisposable: CompositeDisposable = CompositeDisposable()

    fun disposeAll() {
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
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
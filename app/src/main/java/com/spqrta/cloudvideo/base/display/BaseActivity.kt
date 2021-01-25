package com.spqrta.cloudvideo.base.display

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseActivity : AppCompatActivity() {

    private lateinit var compositeDisposable: CompositeDisposable

//    open fun attachCommonDelegates(fragment: BaseFragment<BaseActivity>)
//            : List<BaseFragment.FragmentDelegate<BaseActivity>> {
//        return listOf()
//    }

    abstract val layoutRes: Int

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        compositeDisposable = CompositeDisposable()
        setContentView(layoutRes)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
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



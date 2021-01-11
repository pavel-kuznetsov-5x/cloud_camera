package com.spqrta.camera2demo.base.display

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.spqrta.camera2demo.base.mixins.ErrorToastMixin
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable

abstract class BaseFragment<T : Activity>: Fragment(), ErrorToastMixin {

    private var firstResume = true

    fun mainActivity(): T = activity as T

//    var toolbar: CustomToolbar? = null
    var compositeDisposable: CompositeDisposable = CompositeDisposable()

    open val delegates: MutableList<FragmentDelegate<T>> by lazy {
        mutableListOf<FragmentDelegate<T>>(
//            HideKeyboardDelegate(this)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        firstResume = true
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        delegates.forEach { it.onViewCreated(view) }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        delegates.forEach { it.onDestroyView() }
        disposeAll()
    }

    override fun onResume() {
        super.onResume()
        delegates.forEach { it.onResume() }
        if(firstResume) {
            firstResume = false
            onFirstResume()
        }
    }

    override fun onPause() {
        super.onPause()
        delegates.forEach { it.onPause() }
    }

    override fun onStop() {
        super.onStop()
        delegates.forEach { it.onStop() }
    }

    override fun onDestroy() {
        super.onDestroy()
        delegates.forEach { it.onDestroy() }
        disposeAll()
//        Timber.v("--unsubscribe ${System.identityHashCode(this)}")
//        Timber.v("subs |${System.identityHashCode(BaseFragment.debug!!)}| ${BaseFragment.debug?.compositeDisposable?.size()}")
    }

    open fun onLeave() {
        delegates.forEach { it.onLeave() }
        disposeAll()
//        Timber.v("--unsubscribe ${System.identityHashCode(this)}")
    }

    open fun onFirstResume() {

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        delegates.forEach { it.onSaveState(outState) }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            delegates.forEach { it.onLoadState(savedInstanceState) }
        }
    }

    fun disposeAll() {
        compositeDisposable.dispose()
        compositeDisposable = CompositeDisposable()
    }

    abstract class FragmentDelegate<M: Activity>(protected val fragment: BaseFragment<M>) {
        open fun onViewCreated(view: View) {}
        open fun onResume() {}
        open fun onDestroyView() {}
        open fun onPause() {}
        open fun onStop() {}
        open fun onDestroy() {}
        open fun onLeave() {}
        open fun onSaveState(bundle: Bundle) {}
        open fun onLoadState(bundle: Bundle) {}
        open fun onBackPressed(): Boolean {
            return false
        }
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

    open fun onBackPressed(): Boolean {
        delegates.forEach {
            if(it.onBackPressed()) {
                return true
            }
        }
        return false
    }

}
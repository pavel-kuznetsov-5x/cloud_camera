package com.spqrta.cloudvideo.utility.utils

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import java.util.concurrent.TimeUnit

object TimerUtils {
    fun timer(delay: Int): Single<Long> {
        return timer(delay.toLong())
    }

    fun timer(delay: Long, scheduler: Scheduler = AndroidSchedulers.mainThread()): Single<Long> {
        return Single.timer(delay, TimeUnit.MILLISECONDS, scheduler)
    }

    fun interval(
        delay: Int,
        initialDelay: Int? = null,
        scheduler: Scheduler = AndroidSchedulers.mainThread()
    ): Observable<Long> {
        return if (initialDelay == null) {
            Observable.interval(
                delay.toLong(),
                TimeUnit.MILLISECONDS,
                scheduler
            )
        } else {
            Observable.interval(
                initialDelay.toLong(),
                delay.toLong(),
                TimeUnit.MILLISECONDS,
                scheduler
            )
        }
    }

    fun interval(delay: Long): Observable<Long> {
        return Observable.interval(delay, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
    }
}
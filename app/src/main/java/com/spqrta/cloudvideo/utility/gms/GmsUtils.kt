package com.spqrta.cloudvideo.utility.gms

import com.google.android.gms.tasks.Task
import com.spqrta.cloudvideo.utility.pure.Optional
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject

class TaskFailedException(): Exception()

fun <T> Task<T>.toSingle(): Single<T> {
    val subject = SingleSubject.create<T>()
    addOnCompleteListener {
        if(it.isSuccessful) {
            subject.onSuccess(it.result!!)
        } else {
            subject.onError(it.exception ?: TaskFailedException())
        }
    }
    return subject
}

fun <T : Any?> Task<T>.toSingleNullable(): Single<Optional<T>> {
    val subject = SingleSubject.create<Optional<T>>()
    addOnCompleteListener {
        if(it.isSuccessful) {
            subject.onSuccess(Optional(it.result))
        } else {
            subject.onError(it.exception ?: TaskFailedException())
        }
    }
    return subject
}
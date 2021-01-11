package com.spqrta.camera2demo.utility.gms

import com.google.android.gms.tasks.Task
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
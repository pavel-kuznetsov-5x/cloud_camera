package com.spqrta.cloudvideo.utility.pure

import android.animation.Animator
import android.view.View
import android.view.animation.Animation
import android.view.animation.Animation.AnimationListener

abstract class AbstractSimpleAnimatorListener :
    Animator.AnimatorListener {
    override fun onAnimationEnd(animation: Animator?) {}
    override fun onAnimationStart(animation: Animator) {}
    override fun onAnimationCancel(animation: Animator) {}
    override fun onAnimationRepeat(animation: Animator) {}
}

abstract class AbstractSimpleAnimationListener : AnimationListener {
    override fun onAnimationEnd(animation: Animation?) {}
    override fun onAnimationStart(animation: Animation) {}
    override fun onAnimationRepeat(animation: Animation) {}
}

fun View.showWithFade(
    duration: Int = 300,
    callback: () -> Unit = {}
) {
    visibility = View.VISIBLE
    alpha = 0f
    animate().alpha(1f).setDuration(duration.toLong())
        .setListener(object : AbstractSimpleAnimatorListener() {
            override fun onAnimationEnd(animator: Animator?) {
                alpha = 1f
                callback.invoke()
            }
        })
}

fun View.hideWithFade(
    view: View,
    duration: Int = 200,
    callback: () -> Unit = {}
) {
    view.alpha = 1f
    view.animate().alpha(0f).setDuration(duration.toLong())
        .setListener(object : AbstractSimpleAnimatorListener() {
            override fun onAnimationEnd(animator: Animator?) {
                view.visibility = View.GONE
                callback.invoke()
            }
        })
}

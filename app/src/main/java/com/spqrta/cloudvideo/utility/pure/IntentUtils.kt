package com.spqrta.cloudvideo.utility.pure

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri

object IntentUtils {

    @SuppressLint("MissingPermission")
    fun initiateCall(context: Activity, number: String) {
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"));
        context.startActivity(intent)
    }

    fun openUrl(context: Activity, url: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(browserIntent)
    }

    fun shareText(context: Activity, text: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        context.startActivity(shareIntent)
    }

    fun mailTo(context: Activity, email: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            val mailto = "mailto:$email"
            data = Uri.parse(mailto)
//            putExtra(Intent.EXTRA_SUBJECT, subject)
//            putExtra(Intent.EXTRA_TEXT, text)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }

}
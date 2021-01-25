package com.spqrta.cloudvideo.utility.pure

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat

object NotificationUtils {
    const val NOTIFICATION_CHANNEL_ID = "notification_channel"

    fun sendNotification(
        context: Context,
        notificationId: Int,
        notificationTitle: String?,
        notificationText: String?,
        @DrawableRes notificationIcon: Int,
        activityClass: Class<*>?
    ) {
        val notificationManager = context
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        if (notificationManager != null) {
            val notificationIntent = Intent(context, activityClass)
            notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
            val pendingIntent = PendingIntent.getActivity(
                context, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            val alarmSound = RingtoneManager.getDefaultUri(
                RingtoneManager.TYPE_NOTIFICATION
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationChannel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "Channel", NotificationManager.IMPORTANCE_DEFAULT
                )
                notificationManager.createNotificationChannel(notificationChannel)
            }
            val mNotifyBuilder: NotificationCompat.Builder = NotificationCompat.Builder(
                context, NOTIFICATION_CHANNEL_ID
            )
                .setSmallIcon(notificationIcon)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setAutoCancel(true)
                .setSound(alarmSound)
                .setContentIntent(pendingIntent)
            notificationManager.notify(notificationId, mNotifyBuilder.build())
        }
    }

}
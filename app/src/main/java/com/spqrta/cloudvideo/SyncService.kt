package com.spqrta.cloudvideo

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.spqrta.camera2demo.utility.Logg
import com.spqrta.cloudvideo.base.BaseService
import com.spqrta.cloudvideo.repository.AppRepository
import com.spqrta.cloudvideo.repository.DriveRepository
import java.io.File


class SyncService : BaseService() {
    private val binder = MyBinder()
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager
    private var syncing = false

    override fun onBind(p0: Intent?): IBinder {
        return binder
    }

    inner class MyBinder : Binder() {
        val service: SyncService
            get() = this@SyncService

    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(1, createNotification())
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        syncing = false
        onInit()
        return START_NOT_STICKY
    }

    //todo check multiple calls
    fun onInit() {
        if(!syncing) {
            syncing = true
            DriveRepository
                .getDriveVideos()
                .subscribeManaged({ driveVideos ->
                    val videos = AppRepository.getVideos()
//                    Logg.logListMultiline(driveVideos.files)
//                    Logg.logListMultiline(videos)
                    val text = "${videos.size} files, \n ${driveVideos.files.size} drive files"
                    notificationManager.notify(CODE, createNotification(text))

                    videos.forEach { file ->
                        if (file.name !in driveVideos.files.map { it.name }) {
                            DriveRepository.saveVideo(file).subscribeManaged({
                                Logg.d(file.name)
                            }, {
                                syncing = false
                                throw it//todo
                            })
                        }
                    }
                    syncing = false
                }, {
                    syncing = false
                    throw it//todo
                })
        }
    }

    fun onNewVideo() {
        onInit()
    }

    private fun createNotification(text: String = ""): Notification {
        //todo flag activity duplicates
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, CODE, notificationIntent, 0)
            }

        val CHANNEL_ID = "default"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Foreground Service Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(this, NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }

        notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
        val notification: Notification = notificationBuilder
            .setOnlyAlertOnce(true)
            .setContentTitle(TITLE)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
        return notification
    }

    companion object {
        const val TITLE = "Sync service"
        const val CODE = 1
    }

}
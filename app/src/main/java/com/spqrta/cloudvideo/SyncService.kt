package com.spqrta.cloudvideo

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.spqrta.camera2demo.utility.Logg
import com.spqrta.camera2demo.utility.pure.FileUtils.size
import com.spqrta.cloudvideo.base.BaseService
import com.spqrta.cloudvideo.repository.AppRepository
import hu.akarnokd.rxjava2.interop.ObservableInterop
import io.reactivex.Flowable
import io.reactivex.Observable
import java.io.BufferedReader
import java.io.File
import java.io.FileReader


class SyncService : BaseService() {

    private val binder = MyBinder()
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var notificationManager: NotificationManager

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

    private var syncing = false
    private var isRecording = false
    private var recordingFile: File? = null

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    fun onNewVideo() {
        onInit()
    }

    fun onStartRecording(file: File) {
        recordingFile = file
        isRecording = true
//        object: Thread() {
//            override fun run() {
//                while (isRecording) {
//                    if(isRecording) {
//                        Logg.d(AppRepository.getVideos().last().readBytes().size)
//                    }
//                    sleep(2000)
//                }
//            }
//        }.start()
    }

    fun onStopRecording() {
        isRecording = false
    }

    ///////////////////////////////////////////////////////////////////////////
    // Logic
    ///////////////////////////////////////////////////////////////////////////


    fun onInit() {


//        DriveRepository.saveVideoResumable(AppRepository.getVideos().first()).subscribeManaged {  }
//        val file = AppRepository.getVideos().firstOrNull()
//        val chunkSize = 256*1024
//        Logg.d(file?.size())
//        val stream = FileInputStream(file)
//        try {
//            var res = 0
//            do {
//                val bytes = ByteArray(chunkSize)
//                res = stream.read(bytes)
//                Logg.d(bytes)
//            } while (res != -1)
//        } finally {
//            stream.close()
//        }


//        DriveRepository.initResumableUpload(file)
//                .flatMap {
//                    DriveRepository.uploadChunk(it.uploadId, ByteArray(1))
//                            return@flatMap Single.never<Stub>()
//                }
//                .subscribeManaged {  }
    }

    //todo check multiple calls
//    fun onInit() {
//        //todo background thread
//        if(!syncing) {
//            syncing = true
//            DriveRepository
//                .getDriveVideos()
//                .subscribeManaged({ driveVideos ->
//                    val videos = AppRepository.getVideos()
////                    Logg.logListMultiline(driveVideos.files)
////                    Logg.logListMultiline(videos)
//                    val text = "${videos.size} files, \n ${driveVideos.files.size} drive files"
//                    notificationManager.notify(CODE, createNotification(text))
//
//                    videos.forEach { file ->
//                        if (file.name !in driveVideos.files.map { it.name }) {
//                            DriveRepository.saveVideo(file).subscribeManaged({
//                                Logg.d(file.name)
//                            }, {
//                                syncing = false
//                                Logg.d("error")
////                                it.printStackTrace()
////                                throw it//todo
//                            })
//                        }
//                    }
//                    syncing = false
//                }, {
//                    syncing = false
//                    throw it//todo
//                })
//        }
//    }

    ///////////////////////////////////////////////////////////////////////////
    // Utility
    ///////////////////////////////////////////////////////////////////////////

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
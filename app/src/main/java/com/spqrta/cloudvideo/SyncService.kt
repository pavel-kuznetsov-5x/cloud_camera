package com.spqrta.cloudvideo

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.os.*
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat.getSystemService
import com.spqrta.camera2demo.utility.Logg
import com.spqrta.camera2demo.utility.pure.FileUtils.size
import com.spqrta.cloudvideo.base.BaseService
import com.spqrta.cloudvideo.repository.AppRepository
import com.spqrta.cloudvideo.repository.DriveRepository
import org.threeten.bp.LocalDateTime
import retrofit2.HttpException
import java.io.File
import java.io.FileInputStream

//todo test long video
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
        initSync()
        return START_NOT_STICKY
    }

    private var syncThread: HandlerThread? = null

    private var isRecording = false
    private var recordUploadId: String? = null
    private var recordingFile: File? = null
    private var bytesLoaded = 0L

    ///////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////

    fun onNewVideo() {
        initSync()
    }

    fun onStartRecording(_file: File) {
        recordingFile = _file
        isRecording = true
        initSync()
    }

    fun onStopRecording() {
        //todo check case when this failed
        isRecording = false
    }

    ///////////////////////////////////////////////////////////////////////////
    // Logic
    ///////////////////////////////////////////////////////////////////////////

    private fun initSync() {
        if (syncThread == null) {
            syncThread = HandlerThread("sync").also { it.start() }
        }
        val handler = Handler(syncThread!!.looper)
        handler.post {
            if (isRecording) {
                //check if recording upload initialized
                if (recordUploadId == null) {
                    //todo handle error
                    Logg.d("init recording upload")
                    recordUploadId = DriveRepository.initResumableUpload(recordingFile!!).blockingGet().uploadId
                    handler.post {
                        initSync()
                    }
                } else {
                    val fileSize = recordingFile!!.size()
                    Logg.d("$bytesLoaded / ${fileSize} : ${(bytesLoaded.toFloat() / fileSize * 100).toInt()}%")
                    if (fileSize - bytesLoaded > CHUNK_SIZE) {
                        Logg.d("uploading chunk")
                        uploadFileChunk(recordingFile!!, bytesLoaded, CHUNK_SIZE)
                        bytesLoaded += CHUNK_SIZE
                        handler.post {
                            initSync()
                        }
                    } else {
                        Logg.d("pass")
                        //wait till recording file will be bigger than one chunk
                        handler.postDelayed({
                            initSync()
                        }, 1000)
                    }
                }
            } else {
                //check if recording upload not finished yet
                if (recordingFile != null || recordUploadId != null) {
                    val fileSize = recordingFile!!.size()
                    if (bytesLoaded < fileSize) {
                        Logg.d("upload remaining recording chunk")
                        if (fileSize - bytesLoaded > CHUNK_SIZE) {
                            uploadFileChunk(recordingFile!!, bytesLoaded, CHUNK_SIZE, final = false)
                            bytesLoaded += CHUNK_SIZE
                            handler.post {
                                initSync()
                            }
                        } else {
                            Logg.d("upload remaining recording final chunk")
                            val size = fileSize - bytesLoaded
                            uploadFileChunk(recordingFile!!, bytesLoaded, size, final = true)
                            finishRecordingUpload()
                            handler.post {
                                initSync()
                            }
                        }
                    } else {
                        finishRecordingUpload()
                        handler.post {
                            initSync()
                        }
                    }
                } else {
                    Logg.d("sync other videos")
                    val unsyncedVideos = AppRepository.getUnsyncedVideos()
                    if (unsyncedVideos.isNotEmpty()) {
                        unsyncedVideos
                                .sortedBy {
                                    LocalDateTime.parse(it.name.split(".")[0])
                                }
                                .forEach {
                                    //todo check if already synced
                                    syncVideo(it)
                                }
                    } else {
                        syncThread!!.looper.quitSafely()
                        syncThread = null
                    }
                }
            }
        }
    }

    private fun finishRecordingUpload() {
        recordingFile = null
        recordUploadId = null
    }

    private fun syncVideo(file: File) {
        //todo
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

    @SuppressLint("CheckResult")
    private fun uploadFileChunk(file: File, start: Long, chunkSize: Long, final: Boolean = false) {
        FileInputStream(file).use { stream ->
            stream.channel.position(bytesLoaded)

            val bytes = ByteArray(size = (if (final) {
                file.size() - start
            } else {
                chunkSize
            }).toInt())
            val res = stream.read(bytes)
            check(res == bytes.size)
            try {
                DriveRepository.uploadChunk(
                        recordUploadId!!, bytes,
                        bytesLoaded,
                        finalSize = if (final) {
                            recordingFile!!.size()
                        } else {
                            null
                        }
                ).blockingGet()
            } catch (e: Exception) {
                //todo retry on error
                if (e is HttpException) {
                    Logg.e(e.response()!!.errorBody()!!.string())
                } else {
                    Logg.e(e.toString())
                }
            }
        }
    }

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
        const val CHUNK_BASE_SIZE = 256 * 1024L //256 KB
        const val CHUNK_SIZE = 1 * CHUNK_BASE_SIZE
    }

}
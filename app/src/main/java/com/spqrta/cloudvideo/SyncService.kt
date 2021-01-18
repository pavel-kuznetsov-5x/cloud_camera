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

//todo risk tests
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
    private var recordFolderId: String? = null
    private var recordingFile: File? = null
    private var bytesLoaded = 0L

    //todo clear
    private var partsLoaded = 0

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
            handleSyncMessage(handler)
        }
    }

    private fun handleSyncMessage(handler: Handler) {
        if (isRecording) {
            //check if recording upload initialized
            if (recordFolderId == null) {
                //todo handle error
                recordFolderId = DriveRepository.ensureFolderExists(
                        File(recordingFile!!.name.replace(".mp4", "")),
                        parentId = DriveRepository.videosFolderId
                ).blockingGet()
                Logg.d("recording folder created")
                handler.post {
                    initSync()
                }
            } else {
                val fileSize = recordingFile!!.size()
                Logg.d("$bytesLoaded / ${fileSize} : ${(bytesLoaded.toFloat() / fileSize * 100).toInt()}%")
                if (fileSize - bytesLoaded > CHUNK_SIZE) {
                    if (bytesLoaded == 0L) {
                        Logg.d("skipping first chunk")
                        val mdatPosition = Mp4Utils.findMdat(recordingFile!!)
                        bytesLoaded = Mp4Utils.BOX_SIZE_OFFSET + mdatPosition
                        partsLoaded = 1
                    } else {
                        Logg.d("uploading chunk")
                        uploadFilePart(recordingFile!!, partsLoaded, bytesLoaded, CHUNK_SIZE, recordFolderId!!)
                        partsLoaded++
                        bytesLoaded += CHUNK_SIZE
                    }
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
            if (recordingFile != null || recordFolderId != null) {
                val fileSize = recordingFile!!.size()
                if (bytesLoaded < fileSize) {
                    Logg.d("upload remaining recording chunk")
                    if (fileSize - bytesLoaded > CHUNK_SIZE) {
                        uploadFilePart(recordingFile!!, partsLoaded, bytesLoaded, CHUNK_SIZE, recordFolderId!!)
                        partsLoaded++
                        bytesLoaded += CHUNK_SIZE
                        handler.post {
                            initSync()
                        }
                    } else {
                        uploadFilePart(recordingFile!!, partsLoaded, bytesLoaded, fileSize - bytesLoaded, recordFolderId!!)
                        uploadFirstChunk(recordingFile!!)
                        finishRecordingUpload()
                        handler.post {
                            initSync()
                        }
                    }
                } else {
                    uploadFirstChunk(recordingFile!!)
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

    private fun finishRecordingUpload() {
        recordingFile = null
        recordFolderId = null
    }

    private fun syncVideo(file: File) {
        //todo
    }

    private fun uploadFirstChunk(file: File) {
        val mdat = Mp4Utils.findMdat(file)
        uploadFilePart(file, 0, 0, mdat + Mp4Utils.BOX_SIZE_OFFSET, recordFolderId!!)
    }

    @SuppressLint("CheckResult")
    private fun uploadFilePart(file: File, part: Int, start: Long, chunkSize: Long, parentId: String) {
        FileInputStream(file).use { stream ->
            stream.channel.position(start)

            val bytes = ByteArray(chunkSize.toInt())
            val res = stream.read(bytes)
            check(res == bytes.size)
            try {
                DriveRepository.uploadFileBytes(
                        file.name.split(".")[0] + "_part${part + 1}",
                        bytes,
                        parentId
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

    //todo check error loop
    @Deprecated("")
    @SuppressLint("CheckResult")
    private fun uploadResumableChunk(file: File, start: Long, chunkSize: Long, final: Boolean = false) {
        FileInputStream(file).use { stream ->
            stream.channel.position(start)

            val bytes = ByteArray(size = (if (final) {
                file.size() - start
            } else {
                chunkSize
            }).toInt())
            val res = stream.read(bytes)
            check(res == bytes.size)
            try {
                DriveRepository.uploadChunk(
                        recordFolderId!!, bytes,
                        start,
                        finalSize = if (final) {
                            file.size()
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
package com.spqrta.camera2demo.camera.media

import android.media.MediaRecorder
import android.view.Surface
import com.spqrta.camera2demo.utility.Logg
import com.spqrta.camera2demo.utility.Toaster
import org.threeten.bp.LocalDateTime
import java.io.File
import java.lang.Exception
import java.lang.RuntimeException

open class VideoRecorder(val filesDir: File, val orientation: Int) {
    private lateinit var mediaRecorder: MediaRecorder
    ///storage/emulated/0/Android/data/<package>/files/pic.jpg
    lateinit var videoFile: File

    var isRecording: Boolean = false

    val surface: Surface
        get() {
            return mediaRecorder.surface
        }

    init {
        mediaRecorder = MediaRecorder()
    }

    fun start() {
        if (!isRecording) {
            isRecording = true
            mediaRecorder.start()
        }
    }

    fun stop() {
        isRecording = false
        try {
            mediaRecorder.stop()

            cleanIfResultEmpty()

            mediaRecorder = MediaRecorder()
            initMediaRecorder()
        } catch (e: Exception) {
            Toaster.show("Stop failed: ${e}")
            e.printStackTrace()

            cleanIfResultEmpty()

            mediaRecorder = MediaRecorder()
            initMediaRecorder()
        }
    }

    private fun cleanIfResultEmpty() {
        if(videoFile.length() == 0L) {
            videoFile.delete()
        }
    }

    fun initMediaRecorder() {
        videoFile = File(filesDir, "${LocalDateTime.now()}.mp4")
        mediaRecorder.setOutputFile(videoFile.absolutePath)

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setVideoEncodingBitRate(10000000)

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT)

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        //todo
        mediaRecorder.setVideoSize(640, 480)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setVideoFrameRate(30)

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioSamplingRate(16000)

        mediaRecorder.setOrientationHint(orientation)
        mediaRecorder.prepare()
    }

}
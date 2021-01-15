package com.spqrta.camera2demo.camera

import android.annotation.SuppressLint
import android.media.MediaRecorder
import android.util.Size
import android.view.Surface
import com.spqrta.cloudvideo.MyApplication
import com.spqrta.camera2demo.utility.Toaster
import io.reactivex.subjects.BehaviorSubject
import org.threeten.bp.LocalDateTime
import java.io.File
import java.io.IOException
import java.lang.RuntimeException

//todo sync with camerademo
@Suppress("JoinDeclarationAndAssignment")
@SuppressLint("NewApi")
class VideoCameraWrapper(
    previewSurfaceProvider: () -> Surface,
    rotation: Int = 0,
    requiredAspectRatio: Float? = null,
    requireFrontFacing: Boolean = false
) : BaseCameraWrapper<VideoCameraWrapper.FileCameraResult>(
    previewSurfaceProvider = previewSurfaceProvider,
    rotation = rotation,
    requiredImageAspectRatioHw = requiredAspectRatio,
    requireFrontFacing = requireFrontFacing
) {

    override val subject = BehaviorSubject.create<FileCameraResult>()

    private var mediaRecorder: MediaRecorder
    private var mediaRecorderState: MediaRecorderState = MediaRecorderState.INITIAL

    private val filesDir = MyApplication.VIDEOS_FOLDER

    ///storage/emulated/0/Android/data/<package>/files/pic.jpg
    //todo recreate files?
    private var videoFile: File = File(filesDir, "${LocalDateTime.now()}.mp4")

    private val videoSurface: Surface
        get() = mediaRecorder.surface

    var isRecording: Boolean = false

    init {
        mediaRecorder = MediaRecorder()
        setUpMediaRecorder(mediaRecorder)
    }

    override fun provideImageSize(): Size {
        return chooseCameraSize()
    }

    override fun getAvailableSurfaces(): List<Surface> {
        return mutableListOf<Surface>().apply {
            addAll(super.getAvailableSurfaces())
            add(videoSurface)
        }
    }

    override fun onCaptureSessionCreated() {
        startPreview(mutableListOf<Surface>().apply {
            if (hasPreview) {
                add(previewSurfaceProvider?.invoke()!!)
            }
            add(videoSurface)
        })
    }

    fun startRecording() {
        if (cameraDevice != null && !isRecording) {
            isRecording = true
            if(mediaRecorderState != MediaRecorderState.INITIAL) {
                startPreview(mutableListOf<Surface>().apply {
                    if (hasPreview) {
                        add(previewSurfaceProvider?.invoke()!!)
                    }
                    add(videoSurface)
                    setUpMediaRecorder(mediaRecorder)
                })
            }
            mediaRecorder.start()
        }
    }

    fun stopRecording() {
        if (cameraDevice != null) {
            isRecording = false
            try {
                mediaRecorder.stop()
            } catch (e: RuntimeException) {
                Toaster.show("Stop failed")
            }
//            val oldFile = videoFile
//            subject.onNext(FileCameraResult(oldFile))
            mediaRecorder.reset()
            mediaRecorderState = MediaRecorderState.RESETTED
            subject.onNext(FileCameraResult(videoFile))
        }
    }

    //todo to camera demo
    @Throws(IOException::class)
    private fun setUpMediaRecorder(mediaRecorder: MediaRecorder) {
        videoFile = File(filesDir, "${LocalDateTime.now()}.mp4")
        mediaRecorder.setOutputFile(videoFile.absolutePath)

        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setVideoEncodingBitRate(10000000)

        //todo
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER)

        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

        //todo
        mediaRecorder.setVideoSize(640, 480)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
        mediaRecorder.setVideoFrameRate(30)

        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioSamplingRate(16000)

        mediaRecorder.setOrientationHint(calculateOrientation(rotation, characteristics.sensorOrientation))
        mediaRecorder.prepare()
    }

    class FileCameraResult(file: File)

    enum class MediaRecorderState {
        INITIAL, RESETTED
    }

}
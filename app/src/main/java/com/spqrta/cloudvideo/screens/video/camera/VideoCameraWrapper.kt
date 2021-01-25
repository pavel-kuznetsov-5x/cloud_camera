package com.spqrta.cloudvideo.screens.video.camera

import android.annotation.SuppressLint
import android.util.Size
import android.view.Surface
import com.spqrta.cloudvideo.screens.video.camera.media.VideoRecorder
import com.spqrta.cloudvideo.MyApplication
import io.reactivex.subjects.BehaviorSubject
import java.io.File

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

    private var videoRecorder: VideoRecorder

    private val filesDir = MyApplication.VIDEOS_FOLDER

    val videoFile: File
        get() = videoRecorder.videoFile

    val isRecording: Boolean
        get() = videoRecorder.isRecording

    init {
        videoRecorder = VideoRecorder(filesDir, calculateOrientation(
            rotation,
            characteristics.sensorOrientation
        ))
        videoRecorder.initMediaRecorder()
    }

    override fun provideImageSize(): Size {
        return chooseCameraSize()
    }

    override fun getAvailableSurfaces(): List<Surface> {
        return mutableListOf<Surface>().apply {
            addAll(super.getAvailableSurfaces())
            add(videoRecorder.surface)
        }
    }

    override fun onCaptureSessionCreated() {
        startPreview(mutableListOf<Surface>().apply {
            if (hasPreview) {
                add(previewSurfaceProvider?.invoke()!!)
            }
            add(videoRecorder.surface)
        })
    }

    fun startRecording() {
        if (cameraDevice != null) {
            videoRecorder.start()
        }
    }

    fun stopRecording() {
        videoRecorder.stop()

        createCaptureSession(
            cameraDevice!!,
            getAvailableSurfaces()
        ).subscribeManaged({ session ->
            onCaptureSessionCreated()
        }, {
            subject.onError(it)
        })
    }

    class FileCameraResult(file: File)


}
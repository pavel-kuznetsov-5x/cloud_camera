package com.spqrta.cloudvideo.screens.video

import android.Manifest
import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import com.spqrta.camera2demo.base.delegates.StateDelegate
import com.spqrta.camera2demo.base.display.BaseFragment
import com.spqrta.camera2demo.camera.BaseCameraWrapper
import com.spqrta.camera2demo.camera.VideoCameraWrapper
import com.spqrta.camera2demo.camera.view_wrappers.SurfaceViewWrapper
import com.spqrta.camera2demo.utility.CustomApplication
import com.spqrta.camera2demo.utility.Logg
import com.spqrta.camera2demo.utility.Toaster
import com.spqrta.camera2demo.utility.pure.*
import com.spqrta.cloudvideo.MainActivity
import com.spqrta.cloudvideo.MyApplication
import com.spqrta.cloudvideo.R
import com.spqrta.cloudvideo.repository.FilesRepository
import com.tbruyelle.rxpermissions2.RxPermissions
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_camera.*
import kotlinx.android.synthetic.main.fragment_camera.view.*
import kotlinx.android.synthetic.main.layout_gallery.*
import java.io.File

//todo repeated video
class VideoFragment : BaseFragment<MainActivity>() {

    private val galleryAdapter = GalleryAdapter()

    private lateinit var cameraWrapper: VideoCameraWrapper

    private lateinit var surfaceViewWrapper: SurfaceViewWrapper
    private val permissionsSubject = BehaviorSubject.create<Boolean>()
    private var cameraInitialized = false

    private var frontFacing = false

    private var galleryOpenedState = StateDelegate<Boolean>(this)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_camera, container, false)
        v.cameraView.removeAllViews()

        //todo
        v.cameraView.addView(SurfaceView(mainActivity()).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER)
//            setBackgroundColor(Color.RED)
        })
        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //todo
        bSwitchCamera.hide()

        cameraInitialized = false

        surfaceViewWrapper = SurfaceViewWrapper(cameraView.getChildAt(0) as SurfaceView)

        rvGallery.layoutManager = GridLayoutManager(mainActivity(), 3)
        rvGallery.adapter = galleryAdapter
        galleryAdapter.onItemClickListener = {
            try {
//                val intent = Intent(Intent.ACTION_VIEW)
//                intent.setDataAndType(Uri.parse(it), "video/*")
//                startActivity(intent)
                val videoFile = File(it)
                val fileUri = FileProvider.getUriForFile(
                    CustomApplication.context,
                    "com.spqrta.cloudvideo",
                    videoFile
                )
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(fileUri, "video/*")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) //DO NOT FORGET THIS EVER
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toaster.showException(e)
            }
        }

        bCloseGallery.setOnClickListener {
            hideGallery()
        }

        bClearGallery.setOnClickListener {
            //todo repo
            FileUtils.clear(MyApplication.VIDEOS_FOLDER)
            updateGallery()
            //todo recycler animation
        }

        bShot.setOnClickListener {
            onShotClicked()
        }

        bSwitchCamera.setOnClickListener {
            cameraView.animate().rotationY(
                if (frontFacing) {
                    180f
                } else {
                    0f
                }
            ).setDuration(1000L).start()
//            cameraView.hide()
            cameraView.cameraDistance = Float.MAX_VALUE
            frontFacing = !frontFacing
            cameraWrapper.close()
            initCamera()
            cameraWrapper.open()
//            cameraView.show()

        }

        if(galleryOpenedState.state == true) {
            showGallery(withoutAnimation = true)
        }

        initObservables()
        triggerAskForPermissions()
    }

    override fun onPause() {
        //todo check video
        super.onPause()
        if (cameraInitialized) {
            cameraWrapper.close()
        }
    }

    override fun onResume() {
        super.onResume()
        if (cameraInitialized) {
            cameraWrapper.open()
        }
    }

    private fun onShotClicked() {
        if (!cameraWrapper.isRecording) {
            cameraWrapper.startRecording()
            bShot.setImageResource(R.drawable.ic_shot_recording)
            mainActivity().connection.service?.onStartRecording(cameraWrapper.videoFile)
        } else {
            cameraWrapper.stopRecording()
            bShot.setImageResource(R.drawable.ic_shot)
            mainActivity().connection.service?.onStopRecording()
        }
    }

    private fun initCamera() {
        val rotation = mainActivity().windowManager.defaultDisplay.rotation
        cameraWrapper = VideoCameraWrapper(
            {
                surfaceViewWrapper.surface
            },
            rotation,
//            requiredAspectRatio = 480/640f,
            requireFrontFacing = frontFacing
        )

//        Logg.d(" \n"+cameraWrapper.getAvailablePreviewSizesRegardsOrientation().joinToString ("\n") { it.toStringWh() })

        var previewSize = cameraWrapper.getAvailablePreviewSizesRegardsOrientation().firstOrNull()
        if (previewSize == null) {
            previewSize = cameraWrapper.getSizeRegardsOrientation()
        }

        //todo
//        previewSize = Size(1536, 2048)

        Logg.v("preview size ${previewSize.toStringWh()}")

        val lp = surfaceViewWrapper.surfaceView.layoutParams
        lp.height = (
                surfaceViewWrapper.surfaceView.measuredWidth /
                        (previewSize.width / previewSize.height.toFloat())
                ).toInt()
//        lp.width = 480
//        lp.height = 640
        surfaceViewWrapper.surfaceView.layoutParams = lp

        surfaceViewWrapper.setSurfaceSize(previewSize)

        cameraInitialized = true

//        tvInfo.text = "size: ${cameraWrapper.getSizeRegardsOrientation().toStringWh()}"

        cameraWrapper.focusStateObservable.subscribeManaged {
//            Logg.d(it)
            when (it) {
                is BaseCameraWrapper.Focusing -> {
                    ivFocus.clearColorFilter()
                    ivFocus.show()
                }
                is BaseCameraWrapper.Failed -> {
//                    ivFocus.colorFilter = PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
//                    ivFocus.show()
                    ivFocus.hide()
                }
                else -> {
                    ivFocus.hide()
                }
            }
        }

        cameraWrapper.resultObservable.subscribeManaged({ result ->
            onCameraResult(result)
        }, {
            throw it
        })
    }

    private fun onCameraResult(result: VideoCameraWrapper.FileCameraResult) {
        mainActivity().connection.service?.onNewVideo()
    }

    private fun startImageAnimation(bitmap: Bitmap) {
        ivResult.show()
        ivResult.alpha = 1f
        ivResult.rotation = 0f
        ivResult.x = 0f
        ivResult.y = cameraView.y
        ivResult.scaleX = 1f
        ivResult.scaleY = 1f

        ivResult.setImageBitmap(bitmap)

        val targetX = bGallery.x + bGallery.measuredWidth / 2 - ivResult.measuredWidth / 2
        val targetY =
            (lBottom.y + bGallery.y) + bGallery.measuredHeight / 2 - ivResult.measuredHeight / 2
        val targetScaleX = bGallery.measuredWidth.toFloat() / ivResult.measuredWidth * 0.6f
        val targetScaleY = bGallery.measuredHeight.toFloat() / ivResult.measuredHeight * 0.6f
        val startY = ivResult.y
        val anim = ValueAnimator.ofFloat(1f, 0f)
        anim.interpolator = INTERPOLATOR
        anim.addUpdateListener { valueAnimator ->
            val animatorValue = valueAnimator.animatedValue as Float
            ivResult.scaleX = (animatorValue * (1 - targetScaleX)) + targetScaleX
            ivResult.scaleY = (animatorValue * (1 - targetScaleY)) + targetScaleY
            ivResult.x = (1 - animatorValue) * targetX
            ivResult.y = startY - ((1 - animatorValue) * (startY - targetY))
            ivResult.rotation = (1 - animatorValue) * -360
        }
        anim.duration = ANIM_DURATION
//        anim.addListener(object : AbstractSimpleAnimatorListener() {
//            override fun onAnimationEnd(animation: Animator?) {
//
//            }
//        })
        anim.start()

        ivResult.animate()
            .alpha(0f)
//            .setInterpolator(INTERPOLATOR)
            .setStartDelay(FADE_DELAY)
            .setDuration(FADE_DURATION)
            .withEndAction {
                ivResult.makeInvisible()
                ivResult.setImageBitmap(null)
                bitmap.recycle()
            }
            .start()
    }

    private fun triggerAskForPermissions() {
        RxPermissions(this).requestEach(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ).toList().subscribeManaged { list ->
            if (list.all { it.granted }) {
                permissionsSubject.onNext(true)
            } else {
                //todo handle not granted
                //todo hide gallery
                permissionsSubject.onNext(false)
            }
        }
    }

    private fun initObservables() {
        Observable.combineLatest(
            surfaceViewWrapper.subject,
            permissionsSubject,
            BiFunction<SurfaceViewWrapper.SurfaceViewState, Boolean,
                    Pair<SurfaceViewWrapper.SurfaceViewState, Boolean>> { p1, p2 ->
                Pair(p1, p2)
            }
        ).subscribeManaged { pair: Pair<SurfaceViewWrapper.SurfaceViewState, Boolean> ->
            val surfaceViewState = pair.first
            val permissionsAllowed = pair.second
            if (permissionsAllowed) {
                when (surfaceViewState) {
                    is SurfaceViewWrapper.SurfaceAvailable -> {
                        if (!cameraInitialized) {
                            initCamera()

                            bGallery.setOnClickListener {
                                showGallery()
                            }

                            //todo other update triggers?
                            updateGallery()
                        }
                        cameraWrapper.open()
                    }
                    else -> {
                        //todo
                        cameraWrapper.close()
                        cameraInitialized = false
                    }
                }
            } else {
                //todo
            }
        }
    }

    private fun updateGallery() {
        //todo handle empty
        galleryAdapter.updateItems(FilesRepository.getVideos().map { it.absolutePath })
//        Logg.d(images)
    }

    private fun showGallery(withoutAnimation: Boolean = false) {
        galleryOpenedState.state = true
        ivFocus.hide()
        ivResult.hide()
        lGallery.y = DeviceInfoUtils.getScreenSize(mainActivity()).height.toFloat()
        lGallery.show()
        if(!withoutAnimation) {
            lGallery.animate().y(0f).start()
        } else {
            lGallery.y = 0f
        }
        //todo anim
        updateGallery()
    }

    private fun hideGallery() {
        galleryOpenedState.state = false
        lGallery.animate()
            .y(DeviceInfoUtils.getScreenSize(mainActivity()).height.toFloat())
            .withEndAction {
                lGallery.hide()
            }
            .start()
    }

    override fun onBackPressed(): Boolean {
        return if (lGallery.isVisible) {
            hideGallery()
            true
        } else {
            false
        }
    }

    companion object {
        const val ANIM_DURATION = 700L
        const val FADE_DURATION = 350L
        const val FADE_DELAY = 600L
        val INTERPOLATOR = android.view.animation.AccelerateDecelerateInterpolator()

//        val DEBUG_WIDTH = 720
//        val DEBUG_HEIGHT = 1280

//        val DEBUG_WIDTH = 480
//        val DEBUG_HEIGHT = 640

//        val DEBUG_WIDTH = 1456
//        val DEBUG_HEIGHT = 1456

//        val DEBUG_WIDTH = 5472
//        val DEBUG_HEIGHT = 7296
    }

}
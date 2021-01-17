package com.spqrta.camera2demo.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Size
import android.util.SparseIntArray
import android.view.Surface
import androidx.core.content.ContextCompat
import com.spqrta.camera2demo.utility.CustomApplication
import com.spqrta.camera2demo.utility.Logg
import com.spqrta.camera2demo.utility.Meter
import com.spqrta.cloudvideo.utility.SubscriptionManager
import com.spqrta.camera2demo.utility.pure.aspectRatio
import com.spqrta.camera2demo.utility.pure.toStringWh
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject
import java.nio.ByteBuffer
import java.util.*
import kotlin.math.atan
import kotlin.math.min

//todo check if subject store past values? possible leak
@Suppress("JoinDeclarationAndAssignment")
@SuppressLint("NewApi")
abstract class BaseCameraWrapper<T>(
    protected val rotation: Int = 0,
    //must be invoked only when camera is open
    protected val previewSurfaceProvider: (() -> Surface)? = null,
    protected val requiredPreviewAspectRatioHw: Float? = null,
    protected val requiredImageAspectRatioHw: Float? = null,
    protected val requireFrontFacing: Boolean = false,
    protected val requiredCameraId: String? = null
//    private val analytics: Analytics? = null
) : SubscriptionManager() {

    private val meter = Meter("base")

    protected abstract val subject: BehaviorSubject<T>
    open val resultObservable: Observable<T> by lazy {
        subject.observeOn(AndroidSchedulers.mainThread())
    }

    private val focusSubject = BehaviorSubject.create<FocusState>()
    open val focusStateObservable: Observable<FocusState> =
        focusSubject
            .observeOn(AndroidSchedulers.mainThread())
            .distinctUntilChanged()

    private var cameraManager: CameraManager

    private lateinit var cameraId: String
    protected lateinit var characteristics: CameraCharacteristicsWrapper
    protected var cameraDevice: CameraDevice? = null

    val hasPreview = previewSurfaceProvider != null

    protected val size: Size by lazy {
        provideImageSize()
    }

    protected open fun provideImageSize(): Size {
        return chooseCameraSize(aspectRatioHw = requiredPreviewAspectRatioHw)
    }

    var isNotOpenOrOpening: Boolean = true

    protected lateinit var captureSession: CameraCaptureSession
    protected var sessionInitialized: Boolean = false
    var sessionState: SessionState = Preview
        set(value) {
            field = value
            Logg.v(value)
        }

    protected val captureCallback = object : SimpleCaptureCallback() {
        override fun process(result: CaptureResult, completed: Boolean) {
            processCaptureResult(result, completed)
        }
    }

    protected var mBackgroundHandler: Handler? = null
    private var mBackgroundThread: HandlerThread? = null

    protected val imageReader: ImageReader

    init {
        cameraManager =
            CustomApplication.context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        chooseCamera(requiredCameraId)

        imageReader = createImageReader()

        imageReader.setOnImageAvailableListener({
            mBackgroundHandler?.post {
                onNewFrame(it)
            }
        }, mBackgroundHandler)
    }

    open fun createImageReader(): ImageReader {
        return ImageReader.newInstance(
            size.width,
            size.height,
            ImageFormat.JPEG,
            2
        )
    }
    //for preview surface
    fun getRawSize(): Size {
        return size
    }

    //for views
    fun getSizeRegardsOrientation(): Size {
        return convertSizeRegardsOrientation(size)
    }

    //you have to set preview surface size equal to one of this
    fun getAvailableRawSizes(): List<Size> {
        return characteristics.availableSizes
    }

    fun getAvailableRawPreviewSizes(): List<Size> {
        return getAvailableRawSizes()
            .filter {
                it.aspectRatio() == size.aspectRatio() && it.width <= 2048 && it.height <= 2048
            }
    }

    fun getAvailablePreviewSizesRegardsOrientation(): List<Size> {
        return getAvailableRawPreviewSizes()
            .map { convertSizeRegardsOrientation(it) }
    }

    private fun convertSizeRegardsOrientation(size: Size): Size {
        return when (calculateOrientation(rotation, characteristics.sensorOrientation)) {
            90, 270 -> Size(size.height, size.width)
            else -> size
        }
    }

    protected open fun onNewFrame(imageReader: ImageReader) {
        try {
//            Logg.v("onNewFrame")
            handleImageAndClose(imageReader)
        } catch (e: IllegalStateException) {
            if (e.message?.contains("maxImages") == true) {

            } else if (e.message?.contains("sending message to a Handler on a dead thread") == true) {
//todo search everywhere
//                CustomApplication.analytics().logException(e)
            } else {
                throw e
            }
        }
    }

    protected open fun handleImageAndClose(imageReader: ImageReader) {}

    fun getAvailableCameras(): List<CameraCharacteristicsWrapper> {
        return cameraManager.cameraIdList.map {
            CameraCharacteristicsWrapper(cameraManager.getCameraCharacteristics(it))
        }
    }


    private fun chooseCamera(requiredCameraId: String?) {
        try {
            if (requiredCameraId != null) {
                cameraId = requiredCameraId
                characteristics =
                    CameraCharacteristicsWrapper(cameraManager.getCameraCharacteristics(cameraId))
            } else {
                for (id in cameraManager.cameraIdList) {
                    val chs =
                        CameraCharacteristicsWrapper(cameraManager.getCameraCharacteristics(id))
                    if (requireFrontFacing != chs.isFrontFacing) {
                        continue
                    } else {
                        cameraId = id
                        characteristics = chs
                        return
                    }
                }
                subject.onError(NoCameraError())
            }
        } catch (e: CameraAccessException) {
            subject.onError(e)
        }
    }

    @SuppressLint("MissingPermission")
    fun open() {
        if (isNotOpenOrOpening) {
            try {
                isNotOpenOrOpening = false
                if (ContextCompat.checkSelfPermission(
                        CustomApplication.context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startBackgroundThread()

                    cameraManager.openCamera(
                        cameraId,
                        object : CameraDevice.StateCallback() {
                            override fun onOpened(camera: CameraDevice) {
                                try {
                                    Logg.v("callback: camera opened")
                                    cameraDevice = camera
                                    onCameraOpened(camera)
                                } catch (e: IllegalStateException) {
                                    if (e.message != "Texture destroyed") {
                                        throw e
                                    }
                                }
                            }

                            override fun onClosed(camera: CameraDevice) {
                                isNotOpenOrOpening = true
                                Logg.v("callback: camera closed")
                            }

                            override fun onDisconnected(camera: CameraDevice) {
                                isNotOpenOrOpening = true
                                Logg.v("callback: camera disconnected")
                                cameraDevice = null
                                subject.onError(CameraDisconnected())
                            }

                            override fun onError(camera: CameraDevice, error: Int) {
                                isNotOpenOrOpening = true
                                cameraDevice = null
                                subject.onError(
                                    CameraError(
                                        error
                                    )
                                )
                            }
                        },
                        null
                    );
                } else {
                    subject.onError(NoPermissionsError())
                }

            } catch (e: CameraAccessException) {
                subject.onError(e)
            }
        }
    }

    protected open fun onCameraOpened(camera: CameraDevice) {
        createCaptureSession(
            camera,
            getAvailableSurfaces()
        ).subscribeManaged({ session ->
            onCaptureSessionCreated()
        }, {
            subject.onError(it)
        })
    }

    protected open fun onCaptureSessionCreated() {
        if (hasPreview) {
            startPreview(listOf(previewSurfaceProvider?.invoke()!!))
        }
    }

    fun close() {
        stopBackgroundThread()
        cameraDevice?.let {
            cameraDevice = null
            it.close()
        }
        disposeAll()
    }

    ///////////////////////////////////////////////////////////////////////////
    // Preview
    ///////////////////////////////////////////////////////////////////////////

    protected fun startPreview(surfaces: List<Surface>) {
        try {
            val requestBuilder = createPreviewRequestBuilder(
                cameraDevice!!, surfaces
            )
            sessionState = Preview
            captureSession.setRepeatingRequest(requestBuilder.build(), captureCallback, null)
        } catch (e: CameraAccessException) {
//            CustomApplication.analytics().logException(e)
            subject.onError(e)
        } catch (e: IllegalStateException) {
//            CustomApplication.analytics().logException(e)
            subject.onError(e)
        }
    }


    private fun createPreviewRequestBuilder(
        cameraDevice: CameraDevice,
        surfaces: List<Surface>
    ): CaptureRequest.Builder {
        val requestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        surfaces.forEach {
            requestBuilder.addTarget(it)
        }
        requestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
        )
//        requestBuilder.set(
//            CaptureRequest.CONTROL_AF_TRIGGER,
//            CameraMetadata.CONTROL_AF_TRIGGER_START
//        )
        return requestBuilder
    }

    protected fun createCaptureSession(
        cameraDevice: CameraDevice,
        surfaces: List<Surface>
    ): Single<CameraCaptureSession> {
        val subject = BehaviorSubject.create<CameraCaptureSession>()

        cameraDevice.createCaptureSession(
            surfaces,
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    subject.onNext(session)
                    subject.onComplete()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    subject.onError(SessionConfigureFailedException())
                }
            }, null
        )

        return Single.fromObservable(subject).doOnSuccess {
            captureSession = it
            sessionInitialized = true
            sessionState = Initial
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Photo
    ///////////////////////////////////////////////////////////////////////////

    private fun processCaptureResult(
        result: CaptureResult,
        completed: Boolean
    ) {
        val autoFocusState = result.get(CaptureResult.CONTROL_AF_STATE)
        val autoExposureState = result.get(CaptureResult.CONTROL_AE_STATE)

        when (autoFocusState) {
            CaptureResult.CONTROL_AF_STATE_INACTIVE -> {
            }
            CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN -> {
                focusSubject.onNext(Focusing)
            }
            null,
            CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
            CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED -> {
                focusSubject.onNext(Focused)
            }
            CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED,
            CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED -> {
                focusSubject.onNext(Failed)
            }
        }

        if (sessionState != Preview) {
            Logg.v("process ${sessionState::class.java.simpleName}, AE: ${autoExposureState}, AF: ${autoFocusState}, completed: $completed")
        }

        when (sessionState) {
            is Preview, Initial, MakingShot -> {
//                Logg.v("preview AF: ${autoFocusState}")
            }
            is Precapture -> {
                if (completed) {
                    onPrecaptureFinished()
                } else when (autoFocusState) {
                    null,
                    CaptureResult.CONTROL_AF_STATE_INACTIVE -> {
//                        Logg.v("autoFocus not available")
                        processAutoExposure(autoExposureState)
                    }
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN -> {
//                        Logg.v("autoFocus in progress")
                    }
                    CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED,
                    CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED,
                    CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED -> {
//                        Logg.v("autoFocus finished")
                        processAutoExposure(autoExposureState)
                    }
                    else -> {
                        val e = IllegalStateException("$sessionState $autoFocusState")
                        //todo
                        e.printStackTrace()
//                        CustomApplication.analytics().logException(e)
                    }
                }
            }
        }
    }

    private fun processAutoExposure(autoExposureState: Int?) {
        when (autoExposureState) {
            null,
            CaptureResult.CONTROL_AE_STATE_INACTIVE,
            CaptureResult.CONTROL_AE_STATE_PRECAPTURE,
            CaptureResult.CONTROL_AE_STATE_FLASH_REQUIRED,
            CaptureResult.CONTROL_AE_STATE_CONVERGED,
            CaptureResult.CONTROL_AE_STATE_LOCKED -> {
                onPrecaptureFinished()
            }
            CaptureResult.CONTROL_AE_STATE_SEARCHING -> {
//                Logg.v("autoExposure is in progress: ${autoExposureState}")
            }
            else -> {
                val e = IllegalStateException(
                    "$sessionState $autoExposureState"
                )
                if (CustomApplication.appConfig.debugMode) {
                    throw e
                }
//                CustomApplication.analytics().logException(e)
            }
        }
    }

    private fun onPrecaptureFinished() {
        Logg.v("onPrecaptureFinished")
        sessionState = MakingShot
        captureStillPicture()
    }

    protected fun runPrecaptureSequence() {
        try {
            val requestBuilder = createAutoFocusSequenceRequestBuilder(cameraDevice!!)
            sessionState = Precapture
            captureSession.capture(
                requestBuilder.build(),
                captureCallback,
                mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
//            CustomApplication.analytics().logException(e)
            sessionState =
                Error
            subject.onError(e)
        }

    }

    protected fun captureStillPicture() {
        try {
            val captureBuilder = createPhotoRequestBuilder(
                cameraDevice!!,
                imageReader.surface,
                previewSurface = previewSurfaceProvider?.invoke()
            )

            captureSession.stopRepeating()
            captureSession.abortCaptures()
            captureSession.capture(
                captureBuilder.build(),
                object : CameraCaptureSession.CaptureCallback() {},
                null
            )
            sessionState =
                SavingPicture

        } catch (e: CameraAccessException) {
//            CustomApplication.analytics().logException(e)
            sessionState =
                Error
            subject.onError(e)
        }

    }

    protected fun createAutoFocusSequenceRequestBuilder(cameraDevice: CameraDevice): CaptureRequest.Builder {
        val requestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        previewSurfaceProvider?.invoke()?.let { requestBuilder.addTarget(it) }
        requestBuilder.set(
            CaptureRequest.CONTROL_AF_TRIGGER,
            CameraMetadata.CONTROL_AF_TRIGGER_START
        )
        return requestBuilder
    }

    protected fun createPhotoRequestBuilder(
        cameraDevice: CameraDevice,
        photoSurface: Surface,
        previewSurface: Surface? = null
    ): CaptureRequest.Builder {
        val requestBuilder =
            cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
        previewSurface?.let { requestBuilder.addTarget(it) }
        requestBuilder.addTarget(photoSurface)
        requestBuilder.set(
            CaptureRequest.JPEG_ORIENTATION,
            0
//            calculateOrientation(rotation, orientation)
        )
//        Logg.d("orientation ${rotation} ${orientation} ${calculateOrientation(rotation, orientation)}")
        requestBuilder.set(
            CaptureRequest.CONTROL_AF_MODE,
            CaptureRequest.CONTROL_AF_MODE_AUTO
        )
        return requestBuilder
    }

    protected fun cropResult(bitmap: Bitmap): Bitmap {
        return if (requiredImageAspectRatioHw != null) {
            val requiredHeight = (bitmap.width / requiredImageAspectRatioHw).toInt()
//            check(requiredHeight <= bitmap.height) { "Required height bigger than original" }
            /*Bitmap.createBitmap(
                bitmap,
                0,
                (bitmap.height - requiredHeight) / 2,
                bitmap.width,
                requiredHeight
            )*/
            Bitmap.createBitmap(
                bitmap,
                0,
                0,
                bitmap.width,
                min(requiredHeight, bitmap.height)
            )
        } else {
            bitmap
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    // Utility
    ///////////////////////////////////////////////////////////////////////////

    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
    }

    protected open fun getAvailableSurfaces(): List<Surface> {
        val surfaceList = mutableListOf(imageReader.surface)
        previewSurfaceProvider?.invoke()?.let { surfaceList.add(it) }
        return surfaceList
    }

    fun chooseCameraSize(
        exact: Size? = null,
        exactHeight: Int? = null,
        aspectRatioHw: Float? = null,
        bottomLimit: Int = 0,
        topLimit: Int = Int.MAX_VALUE,
        smallest: Boolean = false
    ): Size {
        val sizes = getAvailableRawSizes()

//        Logg.v(" \n" + sizes.joinToString("\n") { it.toStringWh() })
        Logg.v(" \n" + sizes.sortedBy { it.width }.joinToString("\n") {
            convertSizeRegardsOrientation(it).toStringWh()
        })


        var selectedSizes = sizes
        var selectedSize: Size? = null

        if (exact != null) {
            val requiredExact = convertSizeRegardsOrientation(exact)
            if (sizes.any {
                    it.width == requiredExact.width && it.height == requiredExact.height
                }) {
                selectedSize = requiredExact
            }
        }

        if (selectedSize == null) {

            exactHeight?.let { h ->
                selectedSizes = sizes.filter { it.height == h }
                if (selectedSizes.isNotEmpty()) {
                    selectedSize = selectedSizes[0]
                }
            }
        }

        if (selectedSize == null) {

            val filteredSizes = sizes
                .filter { it.width <= topLimit && it.height <= topLimit }
                .filter { it.width >= bottomLimit && it.height >= bottomLimit }

            //choose by aspect ratio
            selectedSizes = filteredSizes
                .filter {
                    if (aspectRatioHw != null) {
                        it.height / it.width.toFloat() == aspectRatioHw
                    } else true
                }
                .toList()

            if (filteredSizes.isEmpty()) {
                Logg.e("required size not found")
//                CustomApplication.analytics().logException(Exception("size range not found"))
            }

            selectedSize = if (smallest) {
                Collections.min(
                    selectedSizes,
                    SizeComparatorByArea
                )
            } else {
                Collections.max(
                    selectedSizes,
                    SizeComparatorByArea
                )
            }
        }

        Logg.v("selected size ${convertSizeRegardsOrientation(selectedSize!!).toStringWh()}")
        return selectedSize!!
    }

    ///////////////////////////////////////////////////////////////////////////
    // Classes
    ///////////////////////////////////////////////////////////////////////////

    object SizeComparatorByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height)
        }
    }

    open class StubCameraResult
    open class BitmapCameraResult(val bitmap: Bitmap)
    open class BytesCameraResult(val bytes: ByteArray)
    open class FileCameraResult(
        val photoPath: String,
        val videoPath: String? = null
    )

    class CameraDisconnected : Exception()
    class CameraError(private val errorCode: Int) : Exception() {
        override val message: String?
            get() = "Camera Error: code $errorCode"
    }

    class NoPermissionsError : Exception()
    class NoCameraError : Exception()

    open class SessionState {
        override fun toString(): String {
            return "session: ${this::class.java.simpleName}"
        }
    }

    //todo delete
    open class PreviewSurfaceState
    class SurfaceAvailable(val surface: Surface) : PreviewSurfaceState()
    object SurfaceDestroyed : PreviewSurfaceState()

    object Initial : SessionState()
    object Preview : SessionState()
    object MakingShot : SessionState()
    object Precapture : SessionState()
    object SavingPicture : SessionState()
    object PictureSaved : SessionState()
    object Error : SessionState()

    open class FocusState
    object Focusing : FocusState()
    object Focused : FocusState()
    object Failed : FocusState()

    abstract class SimpleCaptureCallback : CameraCaptureSession.CaptureCallback() {

        abstract fun process(result: CaptureResult, completed: Boolean)

        override fun onCaptureProgressed(
            session: CameraCaptureSession,
            request: CaptureRequest,
            partialResult: CaptureResult
        ) {
            process(partialResult, false)
        }

        override fun onCaptureCompleted(
            session: CameraCaptureSession,
            request: CaptureRequest,
            result: TotalCaptureResult
        ) {
            process(result, completed = true)
        }
    }

    class SessionConfigureFailedException : Exception()

    class CameraCharacteristicsWrapper(private val characteristics: CameraCharacteristics) {
        val isFrontFacing: Boolean
            get() {
                return characteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            }

        val fov: Float
            get() {
                val width =
                    characteristics.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.width!!
                val focalLength =
                    characteristics.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)
                        ?.first()!!
                val fov = 2 * atan(width / (focalLength * 2))
                return fov
            }

        val availableSizes: List<Size>
            get() {
                return characteristics.get(
                    CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
                )!!.getOutputSizes(ImageFormat.JPEG).toList()
            }

        val sensorOrientation: Int
            get() {
                return characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
            }
    }

    companion object {

        fun imageToBytesAndClose(image: Image): ByteArray {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            image.close()
            return bytes
        }

        fun imageToBitmapAndClose(image: Image): Bitmap {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            image.close()
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        }

        fun bytesToBitmap(buffer: ByteBuffer): Bitmap {
            val bytes = ByteArray(buffer.capacity())
            buffer.get(bytes)
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
        }

        fun calculateOrientation(screenOrientation: Int, deviceRotation: Int): Int {
            return (DEVICE_TO_SURFACE_ORIENTATION_MAP[screenOrientation] + deviceRotation + 270) % 360
        }

        val DEVICE_TO_SURFACE_ORIENTATION_MAP = SparseIntArray(4).apply {
            append(Surface.ROTATION_0, 90)
            append(Surface.ROTATION_90, 0)
            append(Surface.ROTATION_180, 270)
            append(Surface.ROTATION_270, 180)
        }

    }


}
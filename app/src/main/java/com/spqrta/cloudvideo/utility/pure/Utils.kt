package com.spqrta.camera2demo.utility.pure

import android.app.Activity
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.Base64
import android.util.Size
import android.view.KeyEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.lifecycle.MutableLiveData
import org.json.JSONArray
import org.json.JSONObject
import org.threeten.bp.LocalDate
import com.spqrta.camera2demo.utility.CustomApplication
import com.spqrta.camera2demo.utility.CustomApplication.Companion.context
import java.io.InputStream
import kotlin.math.pow
import kotlin.math.round

object Utils {
    fun copyToClipboard(text: String, showToast: Boolean = false) {
        val manager =
            CustomApplication.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("Source Text", text)
        manager.setPrimaryClip(clipData)
        if (showToast) {
//            Toaster.show(CustomApplication.context.resources.getString(R.string.copied_to_clipboard))
        }
    }

    fun hideKeyboard(activity: Activity) {
        val inputMethodManager =
            activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        activity.currentFocus?.let {
            inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
        }
    }

    fun showKeyboard(activity: Activity, view: View? = null) {
        view?.requestFocus()
        val inputMethodManager =
            context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.showSoftInput(activity.currentFocus, 0)
    }

    fun setStatusBarColor(
        activity: Activity,
        @ColorRes color: Int,
        setLight: Boolean? = null,
        @ColorRes compatColor: Int? = null
    ) {
        if (setLight != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                var flags = activity.window.decorView.systemUiVisibility

                if (setLight) {
                    flags = flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    activity.window.decorView.systemUiVisibility = flags

                } else {
                    flags = flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
                    activity.window.decorView.systemUiVisibility = flags
                }

                activity.window.statusBarColor = activity.resources.getColor(color)

            } else {
                activity.window.statusBarColor = activity.resources.getColor(compatColor ?: color)
            }
        } else {
            activity.window.statusBarColor = activity.resources.getColor(color)
        }
    }

    fun getStringWithDebug(@StringRes res: Int, debugText: String): String {
        return if (CustomApplication.appConfig.releaseMode) {
            CustomApplication.context.getString(res)
        } else {
            debugText
        }
    }

    fun getStringWithDebug(@StringRes res: Int, throwable: Throwable): String {
        return if (CustomApplication.appConfig.releaseMode) {
            CustomApplication.context.getString(res)
        } else {
            "${throwable::class.java.simpleName} ${throwable.message}"
        }
    }

    fun loadJsonFromAssets(filename: String, array: Boolean = false): JSONObject {
        val am = CustomApplication.context.assets
        val inputStream = am.open(filename)
        val json: JSONObject = if (array) {
            val root = JSONObject()
            root.put(
                "values", JSONArray(
                    readFromStream(
                        inputStream
                    )
                )
            )
            root
        } else {
            JSONObject(readFromStream(inputStream))
        }
        return json
    }

    private fun readFromStream(inputStream: InputStream): String {
        var ch: Int = 0
        val sb = StringBuilder()
        while (ch != -1) {
            ch = inputStream.read()
            sb.append(ch.toChar())
        }
        return sb.toString()
    }

    fun isDoneAction(actionId: Int, event: KeyEvent?): Boolean {
        return actionId == EditorInfo.IME_ACTION_DONE
                || actionId == EditorInfo.IME_ACTION_SEARCH
                || actionId == EditorInfo.IME_ACTION_SEND
                || actionId == EditorInfo.IME_ACTION_NEXT
                || (event?.action == KeyEvent.ACTION_UP && event.keyCode == KeyEvent.KEYCODE_ENTER)
    }

    fun setKeepScreenOn(window: Window, disable: Boolean) {
        if (disable) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

class Optional<M>(private val optional: M?) {

    val isEmpty: Boolean
        get() = this.optional == null

    val isNotEmpty: Boolean
        get() = !isEmpty

    fun get(): M {
        if (optional == null) {
            throw NoSuchElementException("No value present")
        }
        return optional
    }

    fun getNullable(): M? {
        return optional
    }

    companion object {
        fun <T> nullValue() = Optional<T>(null)
    }
}

object DatePickerUtil {
    fun createDatePicker(
        context: Context,
        date: LocalDate,
        listener: (LocalDate) -> Unit
    ) {
        return DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                listener.invoke(LocalDate.of(year, month + 1, dayOfMonth))
            },
            date.year,
            date.monthValue - 1,
            date.dayOfMonth
        )
            .show()
    }
}

object Base64Utils {
    fun encode(string: String): String {
        return Base64.encodeToString(string.toByteArray(), Base64.DEFAULT)
    }

    fun decode(string: String): String {
        val data = Base64.decode(string, Base64.DEFAULT)
        return String(data)
    }
}

object Stub : Any()


fun Int.toColorInt(): Int {
    return CustomApplication.context.resources.getColor(this)
}

fun <T : Any?> MutableLiveData<T>.initWith(initialValue: T) = apply { setValue(initialValue) }

fun <T : Any?> MutableLiveData<T>.init(initializer: () -> T) =
    apply { setValue(initializer.invoke()) }

fun <T : Any?> T.nullIf(condition: (T) -> Boolean): T? {
    return this?.let {
        if (condition.invoke(this)) {
            null
        } else {
            this
        }
    }
}

fun Size.toStringWh(): String {
    return "${width}x$height"
}

fun Size.aspectRatio(): Float {
    return width.toFloat()/height
}

fun <T> T?.replaceIfNull(obj: T): T {
    return this ?: obj
}

fun Float.roundTo(decimals: Int): Float {
    return round(this * 10F.pow(decimals)) / 10F.pow(decimals)
}

fun Int?.ifNotNullElse(yes: (Int) -> Unit, els: () -> Unit) {
    if (this != null) {
        yes.invoke(this)
    } else {
        els.invoke()
    }
}

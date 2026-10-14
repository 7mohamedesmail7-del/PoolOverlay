package com.pooloverlay

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView

class SettingsPanel(
    private val context: Context,
    private val config: GeometryEngine.Config,
    private val callback: Callback
) {
    interface Callback {
        fun onClose()
        fun onChanged()
    }

    private var root: View? = null
    private lateinit var wm: WindowManager

    fun show() {
        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E61E1E1E"))
            setPadding(32, 32, 32, 32)
        }

        ll.addView(title("PoolOverlay"))

        ll.addView(toggle("Auto Lengthen Guideline", config.autoLengthen) { config.autoLengthen = it })
        ll.addView(toggle("Cushion Shot Guideline", config.cushionGuideline) { config.cushionGuideline = it })
        ll.addView(toggle("Show 3 Lines", config.show3Lines) { config.show3Lines = it })
        ll.addView(toggle("Show Edges of Ball", config.showEdges) { config.showEdges = it })

        val params = WindowManager.LayoutParams(
            720,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
        }

        wm.addView(ll, params)
        root = ll
    }

    fun hide() {
        root?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        root = null
    }

    private fun title(t: String) = TextView(context).apply {
        text = t
        setTextColor(Color.parseColor("#FFFC00"))
        textSize = 18f
        setPadding(0, 0, 0, 24)
    }

    private fun toggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit): View {
        return Switch(context).apply {
            text = label
            isChecked = initial
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 16)
            setOnCheckedChangeListener { _, v ->
                onChange(v)
                callback.onChanged()
            }
        }
    }
}
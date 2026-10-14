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

    private lateinit var wm: WindowManager
    private var panel: View? = null
    private var gearButton: View? = null
    private var panelVisible = false

    fun show() {
        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createGearButton()
    }

    fun hide() {
        panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        gearButton?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        panel = null
        gearButton = null
    }

    private fun createGearButton() {
        val btn = TextView(context).apply {
            text = "⚙"
            setTextColor(Color.parseColor("#FFFC00"))
            textSize = 26f
            setBackgroundColor(Color.parseColor("#CC000000"))
            setPadding(28, 28, 28, 28)
            setOnClickListener { togglePanel() }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.TOP
            x = 30
            y = 220
        }
        wm.addView(btn, params)
        gearButton = btn
    }

    private fun togglePanel() {
        if (panelVisible) {
            panel?.let { try { wm.removeView(it) } catch (_: Exception) {} }
            panel = null
            panelVisible = false
        } else {
            createPanel()
            panelVisible = true
        }
    }

    private fun createPanel() {
        val ll = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E60F0F0F"))
            setPadding(30, 30, 30, 30)
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, 0, 0, 20)
        }
        val title = TextView(context).apply {
            text = "PoolOverlay v6"
            setTextColor(Color.parseColor("#FFFC00"))
            textSize = 18f
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 20f
            setPadding(20, 0, 20, 0)
            setOnClickListener { togglePanel() }
        }
        header.addView(title)
        header.addView(closeBtn)
        ll.addView(header)

        ll.addView(toggle("Auto Lengthen", config.autoLengthen) { config.autoLengthen = it })
        ll.addView(toggle("Cushion Shot", config.showBounce) { config.showBounce = it })
        ll.addView(toggle("Show 3 Lines", config.show3Lines) { config.show3Lines = it })
        ll.addView(toggle("Stop Point", config.showStopPoint) { config.showStopPoint = it })
        ll.addView(toggle("Physics Path", config.showPhysicsPath) { config.showPhysicsPath = it })
        ll.addView(toggle("Alternatives", config.showAlternatives) { config.showAlternatives = it })
        ll.addView(toggle("Show Edges", config.showEdges) { config.showEdges = it })

        val params = WindowManager.LayoutParams(
            620,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            x = 20
        }

        wm.addView(ll, params)
        panel = ll
    }

    private fun overlayType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

    private fun toggle(label: String, initial: Boolean, onChange: (Boolean) -> Unit): View {
        return Switch(context).apply {
            text = label
            isChecked = initial
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 12, 0, 12)
            setOnCheckedChangeListener { _, v -> onChange(v); callback.onChanged() }
        }
    }
}
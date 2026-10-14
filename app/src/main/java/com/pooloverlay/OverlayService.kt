package com.pooloverlay

import android.app.*
import android.content.Intent
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class OverlayService : Service() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private lateinit var windowManager: WindowManager
    private var overlayView: OverlayView? = null
    private var settingsPanel: SettingsPanel? = null

    private val capturing = AtomicBoolean(false)
    private val uiHandler = Handler(Looper.getMainLooper())
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler

    private val detector = BallDetector()
    private val tracker = BallTracker()
    private val engine = GeometryEngine()

    private val frameCounter = AtomicInteger(0)
    private val FRAME_SKIP = 2
    @Volatile private var lastDetectionMs = 0L
    private val MIN_DETECTION_INTERVAL = 60L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        workerThread = HandlerThread("PoolOverlayWorker").apply { start() }
        workerHandler = Handler(workerThread.looper)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val resultCode = intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED)
            ?: return START_NOT_STICKY
        val data: Intent? = if (Build.VERSION.SDK_INT >= 33)
            intent.getParcelableExtra("data", Intent::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra("data")
        if (data == null) return START_NOT_STICKY

        if (projection == null) {
            val mpm = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            projection = mpm.getMediaProjection(resultCode, data)
            projection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() { stopSelf() }
            }, uiHandler)
            setupCapture()
            setupOverlay()
        }
        return START_STICKY
    }

    private fun setupCapture() {
        val dm = resources.displayMetrics
        val w = dm.widthPixels / 2
        val h = dm.heightPixels / 2

        imageReader = ImageReader.newInstance(w, h, PixelFormat.RGBA_8888, 2)
        virtualDisplay = projection?.createVirtualDisplay(
            "PoolOverlay",
            w, h, dm.densityDpi / 2,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, uiHandler
        )

        capturing.set(true)
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            val count = frameCounter.incrementAndGet()
            if (count % FRAME_SKIP != 0) { image.close(); return@setOnImageAvailableListener }
            val now = System.currentTimeMillis()
            if (now - lastDetectionMs < MIN_DETECTION_INTERVAL) {
                image.close(); return@setOnImageAvailableListener
            }
            lastDetectionMs = now
            workerHandler.post {
                try { processFrame(image) } finally { image.close() }
            }
        }, uiHandler)
    }

    private fun processFrame(image: Image) {
        if (!capturing.get()) return
        val bitmap = ImageUtils.toBitmap(image) ?: return

        val rawBalls = detector.detect(bitmap)
        val table = detector.detectTableBounds(bitmap)
        val balls = tracker.update(rawBalls)
        val aim = engine.computeAim(balls, table)

        bitmap.recycle()
        uiHandler.post { overlayView?.update(aim) }
    }

    private fun setupOverlay() {
        overlayView = OverlayView(this)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        windowManager.addView(overlayView, params)

        settingsPanel = SettingsPanel(this, engine.config, object : SettingsPanel.Callback {
            override fun onClose() {}
            override fun onChanged() { uiHandler.post { overlayView?.invalidate() } }
        })
        settingsPanel?.show()
    }

    private fun buildNotification(): Notification {
        val channelId = "pool_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(channelId, "PoolOverlay", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("PoolOverlay v6 يعمل")
            .setContentText("Physics Engine + Advisor")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .build()
    }

    override fun onDestroy() {
        capturing.set(false)
        settingsPanel?.hide()
        overlayView?.let { try { windowManager.removeView(it) } catch (_: Exception) {} }
        detector.release()
        virtualDisplay?.release()
        projection?.stop()
        imageReader?.close()
        workerThread.quitSafely()
        super.onDestroy()
    }
}
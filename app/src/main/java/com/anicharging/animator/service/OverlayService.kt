package com.anicharging.animator.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.anicharging.animator.battery.BatteryHelper
import com.anicharging.animator.overlay.AnimationOverlayView
import com.anicharging.animator.settings.AppSettings
import com.anicharging.animator.settings.PositionPreset
import com.anicharging.animator.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class OverlayService : Service() {

    companion object {
        const val ACTION_SHOW_OVERLAY = "com.anicharging.animator.SHOW_OVERLAY"
        const val ACTION_HIDE_OVERLAY = "com.anicharging.animator.HIDE_OVERLAY"
    }

    private var windowManager: WindowManager? = null
    private var overlayView: AnimationOverlayView? = null
    private var params: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private lateinit var settingsRepository: SettingsRepository
    private var currentSettings: AppSettings = AppSettings()

    private val handler = Handler(Looper.getMainLooper())
    private var isOverlayShowing = false

    private val animRunnable = object : Runnable {
        override fun run() {
            overlayView?.advanceFrame()
            val fpsDelay = if (currentSettings.performanceMode) 100L else 50L // 10 fps vs 20 fps
            handler.postDelayed(this, fpsDelay)
        }
    }

    private val timerRunnable = Runnable {
        hideOverlay()
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        settingsRepository = SettingsRepository(this)

        serviceScope.launch {
            settingsRepository.settingsFlow.collectLatest { settings ->
                currentSettings = settings
                overlayView?.updateSettings(settings)
                updateOverlayPosition(settings)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW_OVERLAY -> showOverlay()
            ACTION_HIDE_OVERLAY -> hideOverlay()
        }
        return START_NOT_STICKY
    }

    private fun showOverlay() {
        if (isOverlayShowing) return

        val view = AnimationOverlayView(this)
        this.overlayView = view

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val p = WindowManager.LayoutParams(
            800,
            900,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        p.gravity = Gravity.CENTER
        this.params = p

        setupDragTouchListener(view, p)

        try {
            windowManager?.addView(view, p)
            isOverlayShowing = true
            view.updateSettings(currentSettings)
            view.updateBatteryInfo(BatteryHelper.getBatteryInfo(this))

            handler.post(animRunnable)

            // Duration check: if > 0 seconds, auto-dismiss after timer
            if (currentSettings.displayDurationSeconds > 0) {
                handler.removeCallbacks(timerRunnable)
                handler.postDelayed(timerRunnable, currentSettings.displayDurationSeconds * 1000L)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setupDragTouchListener(view: View, params: WindowManager.LayoutParams) {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        view.setOnTouchListener { _, event ->
            if (currentSettings.positionPreset != PositionPreset.CUSTOM_DRAG) return@setOnTouchListener false

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(view, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    serviceScope.launch {
                        settingsRepository.updateCustomPosition(params.x.toFloat(), params.y.toFloat())
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun updateOverlayPosition(settings: AppSettings) {
        val p = params ?: return
        when (settings.positionPreset) {
            PositionPreset.STATUS_AREA -> {
                p.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                p.y = 80
            }
            PositionPreset.CENTER -> {
                p.gravity = Gravity.CENTER
                p.x = 0
                p.y = 0
            }
            PositionPreset.CUSTOM_DRAG -> {
                p.gravity = Gravity.CENTER
                p.x = settings.customX.toInt()
                p.y = settings.customY.toInt()
            }
        }
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager?.updateViewLayout(overlayView, p)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun hideOverlay() {
        handler.removeCallbacks(animRunnable)
        handler.removeCallbacks(timerRunnable)
        if (isOverlayShowing && overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            overlayView = null
            isOverlayShowing = false
        }
        stopSelf()
    }

    override fun onDestroy() {
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

package com.okihann.GSTC

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat

class OverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View
    private lateinit var params: WindowManager.LayoutParams
    private val handler = Handler(Looper.getMainLooper())
    private var isUpdating = false
    
    private var fpsCount = 0
    private var lastFpsTimestamp = 0L
    private var activeFpsValue = 60

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            fpsCount++
            if (lastFpsTimestamp == 0L) {
                lastFpsTimestamp = frameTimeNanos
            } else {
                val delta = frameTimeNanos - lastFpsTimestamp
                if (delta >= 1_000_000_000L) {
                    activeFpsValue = fpsCount
                    fpsCount = 0
                    lastFpsTimestamp = frameTimeNanos
                }
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("InflateParams", "ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        val notification = NotificationCompat.Builder(this, "GSTC_CHANNEL")
            .setContentTitle("GSTC Thermal Engine")
            .setContentText("Telemetry overlay is actively running.")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
            
        startForeground(1, notification)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 100
        }

        val inflater = LayoutInflater.from(this)
        overlayView = inflater.inflate(R.layout.overlay_telemetry_layout, null)
        
        setupDraggableOverlay()

        windowManager.addView(overlayView, params)
        Choreographer.getInstance().postFrameCallback(frameCallback)
        startTelemetryLoop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "GSTC_CHANNEL",
                "Thermal Engine Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDraggableOverlay() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView.setOnTouchListener { _, event ->
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
                    windowManager.updateViewLayout(overlayView, params)
                    true
                }
                else -> false
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun startTelemetryLoop() {
        val fpsText = overlayView.findViewById<TextView>(R.id.text_fps)
        val cpuText = overlayView.findViewById<TextView>(R.id.text_cpu)
        val gpuText = overlayView.findViewById<TextView>(R.id.text_gpu)
        val battText = overlayView.findViewById<TextView>(R.id.text_batt)

        handler.post(object : Runnable {
            override fun run() {
                if (isUpdating) {
                    handler.postDelayed(this, 1000)
                    return
                }

                isUpdating = true

                Thread {
                    val stats = HardwareMonitor.fetchAllStats()

                    SessionLogger.logData(
                        activeFpsValue, 
                        stats.cpuUsage, 
                        stats.cpuTemp, 
                        stats.gpuUsage, 
                        stats.gpuFreq, 
                        stats.gpuTemp, 
                        stats.battTemp
                    )

                    handler.post {
                        fpsText.text = "FPS: $activeFpsValue"
                        cpuText.text = "CPU: ${stats.cpuUsage}% (${stats.cpuTemp}°C)"
                        gpuText.text = "GPU: ${stats.gpuUsage}% | ${stats.gpuFreq}MHz (${stats.gpuTemp}°C)"
                        battText.text = "BATT: ${stats.battTemp}°C"
                        isUpdating = false
                    }
                }.start()
                handler.postDelayed(this, 1000)
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        if (::overlayView.isInitialized) windowManager.removeView(overlayView)
    }
}
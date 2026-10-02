package com.anicharging.animator.overlay

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.anicharging.animator.battery.BatteryInfo
import com.anicharging.animator.settings.AppSettings
import com.anicharging.animator.settings.BatteryBarDesign
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnimationOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var frames: List<Bitmap> = emptyList()
    private var currentFrameIndex = 0
    private var rotationAngle = 0f

    private var settings: AppSettings = AppSettings()
    private var batteryInfo: BatteryInfo = BatteryInfo(50, true, 30.0f, 86.0f, "Fast Charging", "AC Charger")

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 36f
        textAlign = Paint.Align.CENTER
    }

    private val batteryBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val batteryBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#444444")
        style = Paint.Style.FILL
    }

    private val dateFormat = SimpleDateFormat("EEE, MMM d • HH:mm", Locale.getDefault())

    fun updateSettings(newSettings: AppSettings) {
        this.settings = newSettings
        loadFramesForCategory(newSettings.animationCategory)
        invalidate()
    }

    fun updateBatteryInfo(info: BatteryInfo) {
        this.batteryInfo = info
        invalidate()
    }

    private fun loadFramesForCategory(category: String) {
        val loaded = mutableListOf<Bitmap>()
        try {
            val assetManager = context.assets
            val files = assetManager.list(category) ?: emptyArray()
            for (file in files.sorted()) {
                if (file.endsWith(".png") || file.endsWith(".jpg")) {
                    val inputStream = assetManager.open("$category/$file")
                    val b = BitmapFactory.decodeStream(inputStream)
                    inputStream.close()
                    if (b != null) loaded.add(b)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.frames = loaded
    }

    fun advanceFrame() {
        if (frames.isNotEmpty()) {
            currentFrameIndex = (currentFrameIndex + 1) % frames.size
        }
        rotationAngle = (rotationAngle + 3f) % 360f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f - 60f

        // 1. Draw Animation Eye / Effect Frame
        if (frames.isNotEmpty()) {
            val frame = frames[currentFrameIndex % frames.size]
            val scale = settings.animationScale
            val baseSize = 240f * scale
            val left = cx - baseSize / 2f
            val top = cy - baseSize / 2f
            val rect = RectF(left, top, left + baseSize, top + baseSize)

            canvas.save()
            // Continuous rotation effect for sharingan/energy
            canvas.rotate(rotationAngle, cx, cy)
            canvas.drawBitmap(frame, null, rect, paint)
            canvas.restore()
        }

        // 2. Draw Battery Bar & Sharingan Icon beside Battery Bar
        val barWidth = 320f * settings.animationScale
        val barHeight = 24f
        val barLeft = cx - barWidth / 2f
        val barTop = cy + 150f
        val barRight = barLeft + barWidth
        val barBottom = barTop + barHeight

        val rx = 12f
        val ry = 12f

        // Draw background
        canvas.drawRoundRect(barLeft, barTop, barRight, barBottom, rx, ry, batteryBgPaint)

        // Draw fill based on percentage and design
        val fillWidth = (barWidth * (batteryInfo.level / 100f)).coerceAtLeast(0f)
        val fillRect = RectF(barLeft, barTop, barLeft + fillWidth, barBottom)

        when (settings.batteryBarDesign) {
            BatteryBarDesign.RED_SHARINGAN -> {
                batteryBarPaint.color = Color.parseColor("#FFE50914") // Red Sharingan
            }
            BatteryBarDesign.NORMAL -> {
                batteryBarPaint.color = if (batteryInfo.level <= 20) Color.RED else Color.GREEN
            }
            BatteryBarDesign.COMBINED -> {
                batteryBarPaint.color = if (batteryInfo.level <= 20) Color.RED else Color.parseColor("#FF00E5FF")
            }
        }
        canvas.drawRoundRect(fillRect, rx, ry, batteryBarPaint)

        // Draw Tiny Sharingan Eye Beside Battery Bar if available
        if (frames.isNotEmpty()) {
            val sharinganIcon = frames[currentFrameIndex % frames.size]
            val iconSize = 36f
            val iconLeft = barLeft - iconSize - 12f
            val iconTop = barTop + (barHeight - iconSize) / 2f
            val iconRect = RectF(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)

            canvas.save()
            canvas.rotate(rotationAngle, iconLeft + iconSize / 2f, iconTop + iconSize / 2f)
            canvas.drawBitmap(sharinganIcon, null, iconRect, paint)
            canvas.restore()
        }

        // 3. Draw Overlay Text Info (Battery %, Temp, Charging Speed, Date/Time)
        var textY = barBottom + 50f

        textPaint.textSize = 42f
        canvas.drawText("${batteryInfo.level}% Charging", cx, textY, textPaint)
        textY += 45f

        textPaint.textSize = 30f
        if (settings.showChargingSpeed) {
            canvas.drawText(batteryInfo.chargingSpeed, cx, textY, textPaint)
            textY += 38f
        }

        if (settings.showTemperature) {
            val tempStr = String.format(Locale.US, "%.1f°C / %.1f°F", batteryInfo.temperatureCelsius, batteryInfo.temperatureFahrenheit)
            canvas.drawText(tempStr, cx, textY, textPaint)
            textY += 38f
        }

        if (settings.showDateTime) {
            val dateStr = dateFormat.format(Date())
            canvas.drawText(dateStr, cx, textY, textPaint)
        }
    }
}

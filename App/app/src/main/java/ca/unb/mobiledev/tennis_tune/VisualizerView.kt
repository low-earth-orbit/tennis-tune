package ca.unb.mobiledev.tennis_tune

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class VisualizerView : View {
    private val paint = Paint()
    private var amplitudes: ByteArray = ByteArray(0)
    private var isAudioInputAvailable = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        paint.color =
            resources.getColor(android.R.color.holo_blue_light, null) // Updated way to getColor
        paint.style = Paint.Style.FILL
    }

    fun updateVisualizer(newAmplitudes: ByteArray) {
        amplitudes = newAmplitudes
        invalidate()  // Request a redraw.
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isAudioInputAvailable && amplitudes.isNotEmpty()) {
            val barWidth = width / amplitudes.size.toFloat()
            amplitudes.forEachIndexed { index, amplitude ->
                val x = index * barWidth
                val normalizedAmplitude = (amplitude.toInt() + 128) / 256f
                val heightAmplitude = normalizedAmplitude * height
                canvas.drawRect(x, height - heightAmplitude, x + barWidth, height.toFloat(), paint)
            }
        }
    }

    fun setAudioInputAvailable(isAvailable: Boolean) {
        isAudioInputAvailable = isAvailable
        if (!isAvailable) {
            amplitudes = ByteArray(0)
        }
        invalidate()
    }
}

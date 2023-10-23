package ca.unb.mobiledev.tennis_tune

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class VisualizerView : View {
    private val paint = Paint()
    private var amplitudes = ByteArray(0)
    private var isAudioInputAvailable = false

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        paint.color = resources.getColor(android.R.color.holo_blue_light)
    }

    fun updateVisualizer(amplitudes: ByteArray) {
        this.amplitudes = amplitudes
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isAudioInputAvailable) {
            for (i in amplitudes.indices) {
                val x = i * (width / amplitudes.size).toFloat()
                val y = (amplitudes[i] + 128) * (height / 256f)
                canvas.drawRect(
                    x,
                    height.toFloat() - y,
                    x + (width / amplitudes.size).toFloat(),
                    height.toFloat(),
                    paint
                )
            }
        }
    }

    fun setAudioInputAvailable(isAvailable: Boolean) {
        isAudioInputAvailable = isAvailable
    }
}

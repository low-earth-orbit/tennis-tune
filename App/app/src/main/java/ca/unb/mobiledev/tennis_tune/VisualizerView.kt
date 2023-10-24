package ca.unb.mobiledev.tennis_tune

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.View
import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.absoluteValue

class VisualizerView : View {
    private val paint = Paint()
    private var amplitudes: ByteArray = ByteArray(0)
    private var isAudioInputAvailable = false
    private var fft = FloatFFT_1D(1024) // Assuming 1024 samples
    private var magnitudes = FloatArray(512) // Magnitudes (after FFT)
    private var maxFrequency: Float = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val resetFrequencyRunnable = Runnable {
        maxFrequency = 0f
        invalidate()
    }
    private val sampleRate =
        44100  // For example, typical CD quality audio uses a sample rate of 44.1 kHz

    var dominantFrequency: Float = 0f
        private set

    interface OnDominantFrequencyChangedListener {
        fun onDominantFrequencyChanged(frequency: Float)
    }

    var dominantFrequencyListener: OnDominantFrequencyChangedListener? = null

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        paint.color = resources.getColor(android.R.color.white, null)
        paint.style = Paint.Style.FILL
    }

    fun updateVisualizer(newAmplitudes: ByteArray) {
        amplitudes = newAmplitudes
        computeFFT(amplitudes)
        // Find the index with the maximum amplitude after FFT
        val maxIndex =
            amplitudes.indices.maxByOrNull { (amplitudes[it].toInt() and 0xFF).absoluteValue } ?: -1
        if (maxIndex != -1) {
            // Convert index to frequency: frequency = index * sampleRate / FFT_Size
            // NOTE: You might need to adjust this based on your specific FFT implementation details
            maxFrequency =
                (maxIndex * sampleRate / (2 * amplitudes.size)).toFloat() // The '2' assumes the FFT size is twice the amplitude array size
            // Inside the updateVisualizer method, after setting the maxFrequency
            dominantFrequency = maxFrequency
            dominantFrequencyListener?.onDominantFrequencyChanged(maxFrequency)
            Log.d("VisualizerView", "Dominant Frequency: $maxFrequency")
        }

        invalidate()  // Request a redraw

        // Schedule the reset after your desired interval (e.g., 5 seconds)
        handler.removeCallbacks(resetFrequencyRunnable)
        handler.postDelayed(resetFrequencyRunnable, 10000)
    }

    private fun computeFFT(amplitudes: ByteArray) {
        // Convert byte array to float array
        val floatData = FloatArray(1024) { i -> amplitudes.getOrElse(i) { 0 }.toFloat() }

        // Apply FFT
        fft.realForward(floatData)

        // Compute magnitude for each frequency bin
        for (i in 0 until magnitudes.size) {
            val real = floatData[2 * i]
            val imaginary = floatData[2 * i + 1]
            magnitudes[i] = kotlin.math.sqrt(real * real + imaginary * imaginary)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK) // Set the background color to black

        if (isAudioInputAvailable && magnitudes.isNotEmpty()) {
            val numberOfBars = 32 // for example, which is a fraction of the original 512
            val step = magnitudes.size / numberOfBars
            val barWidth = width / numberOfBars.toFloat()
            val scale = 0.1f // Adjust this value to control the vertical scale

            for (i in 0 until numberOfBars) {
                val index = i * step
                val magnitude = magnitudes[index]
                val x = i * barWidth
                val normalizedMagnitude = magnitude / (256f * scale) // Adjusted normalization
                val heightMagnitude = normalizedMagnitude * height
                canvas.drawRect(
                    x,
                    0f,
                    x + barWidth,
                    heightMagnitude,
                    paint
                ) // Drawing from top to magnitude height
            }
//            paint.color = Color.LTGRAY
//            paint.textSize = 40f
            // Adjust the Y-coordinate to place text at a position that is above the tallest bar
//            val textYPosition = 30f  // You can adjust this value as needed
//            canvas.drawText("Max Frequency: ${maxFrequency / 1000} kHz", 10f, textYPosition, paint)
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

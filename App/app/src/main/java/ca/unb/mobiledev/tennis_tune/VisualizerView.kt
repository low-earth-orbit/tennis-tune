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
    private var dominantFrequency: Float = 0f
    private var prevMagnitudes = FloatArray(512) // Holds the previous frame's magnitudes
    private var recentDisplayFrequencies = mutableListOf<Float>()
    private val maxDisplayRecentSize =
        10  // Use a rolling average of the last 10 frequencies. Adjust as needed

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
        val maxIndex = magnitudes.indices.maxByOrNull { magnitudes[it] } ?: -1
        if (maxIndex != -1) {
            maxFrequency = (maxIndex * sampleRate / (2 * magnitudes.size)).toFloat()
            if (maxFrequency in 420f..770f) {
                dominantFrequency = computeDisplayAverageFrequency(maxFrequency)
                dominantFrequencyListener?.onDominantFrequencyChanged(dominantFrequency)
                Log.d("VisualizerView", "Dominant Frequency: $dominantFrequency")
            }
        }
        invalidate()  // Request a redraw
        // Schedule the reset after desired interval (e.g., 1 second)
        handler.removeCallbacks(resetFrequencyRunnable)
        handler.postDelayed(resetFrequencyRunnable, 1000)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK) // Set the background color to black

        if (isAudioInputAvailable && magnitudes.isNotEmpty()) {
            val numberOfBars = 32 // for example, which is a fraction of the original 512
            val step = magnitudes.size / numberOfBars
            val barWidth = width / numberOfBars.toFloat()
            val scale = 0.5f // Adjust this value to control the vertical scale

            val alpha = 0.1f  // Change this value to control the speed of interpolation

            for (i in 0 until numberOfBars) {
                magnitudes[i] = alpha * magnitudes[i] + (1 - alpha) * prevMagnitudes[i]

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
            prevMagnitudes = magnitudes.copyOf()

        }
    }

    private fun hammingWindow(data: FloatArray): FloatArray {
        val N = data.size
        for (i in 0 until N) {
            data[i] *= (0.54 - 0.46 * Math.cos(2 * Math.PI * i / (N - 1))).toFloat()
        }
        return data
    }

    private fun computeFFT(amplitudes: ByteArray) {
        // Convert byte array to float array
        val floatData = FloatArray(1024) { i -> amplitudes.getOrElse(i) { 0 }.toFloat() }

        // Apply the Hamming window
        hammingWindow(floatData)

        // Apply FFT
        fft.realForward(floatData)

        // Compute magnitude for each frequency bin
        for (i in magnitudes.indices) {
            val real = floatData[2 * i]
            val imaginary = floatData[2 * i + 1]
            magnitudes[i] = kotlin.math.sqrt(real * real + imaginary * imaginary)
        }
    }

    private fun computeDisplayAverageFrequency(frequency: Float): Float {
        synchronized(recentDisplayFrequencies) {
            if (recentDisplayFrequencies.size >= maxDisplayRecentSize) {
                recentDisplayFrequencies.removeAt(0)
            }
            recentDisplayFrequencies.add(frequency)
            return recentDisplayFrequencies.average().toFloat()
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

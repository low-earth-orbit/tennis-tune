package ca.unb.mobiledev.tennis_tune

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.max
import kotlin.math.min

class VisualizerView : View {
    private val paint = Paint()
    private var isAudioInputAvailable = false

    private val fftSize = 16384
    private val fftSizeHalf = fftSize / 2
    private var floatData = FloatArray(fftSize)
    private var fft = FloatFFT_1D(fftSize.toLong())
    private var magnitudes = FloatArray(fftSizeHalf)
    private var maxFrequency: Float = 0f
    private var amplitudes = ByteArray(0)

    private val handler = Handler(Looper.getMainLooper())
    private val resetFrequencyRunnable = Runnable {
        maxFrequency = 0f
        invalidate()
    }
    private val sampleRate =
        44100  // For example, typical CD quality audio uses a sample rate of 44.1 kHz
    private var dominantFrequency: Float = 0f
    private var recentDisplayFrequencies = mutableListOf<Float>()
    private val maxDisplayRecentSize =
        60  // Use a rolling window of frequencies

    private var recentMagnitudesAverage = mutableListOf<Float>()
    private val maxMagnitudeAverageSize = fftSizeHalf  // For calculating background
    // noise

    interface OnDominantFrequencyChangeListener {
        fun onDominantFrequencyChange(frequency: Float)
    }

    var dominantFrequencyListener: OnDominantFrequencyChangeListener? = null

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
        // Accumulate audio amplitudes samples until we have enough for an FFT
        amplitudes += newAmplitudes

        if (amplitudes.size < fftSize) {
            return // Not enough samples yet
        }

        // Assuming a 50% overlap for simplicity
        val combinedSize = fftSize
        val combinedAmplitudes = amplitudes.sliceArray(0 until combinedSize)

        // Store the half of the current amplitudes for use in the next call
        amplitudes =
            amplitudes.sliceArray(newAmplitudes.size until amplitudes.size)

        // Compute FFT
        computeFFT(combinedAmplitudes)

        // Apply a thresholding method to filter out background noise
        // Compute the rolling average of magnitudes
        val averageMagnitude = magnitudes.average().toFloat()
        synchronized(recentMagnitudesAverage) {
            if (recentMagnitudesAverage.size >= maxMagnitudeAverageSize) {
                recentMagnitudesAverage.removeAt(0)
            }
            recentMagnitudesAverage.add(averageMagnitude)
        }

        val noiseThreshold = recentMagnitudesAverage.average()
            .toFloat() * 2.0  // Using 2 as a multiplier, adjust as needed

        // Find the index with the maximum amplitude after FFT that's above the noise threshold
        val maxIndex = magnitudes.indices.filter { magnitudes[it] > noiseThreshold }
            .maxByOrNull { magnitudes[it] } ?: -1

        if (maxIndex != -1) {
            maxFrequency = (maxIndex * sampleRate / (2 * magnitudes.size)).toFloat()

            // Apply a frequency range filter
            if (maxFrequency in 420f..770f) {
                // Use median frequency measured to enhance the reliability of measurement
                // Median is less sensitive to outliers than mean
                dominantFrequency = computeDisplayMedianFrequency(maxFrequency)
                dominantFrequencyListener?.onDominantFrequencyChange(dominantFrequency)
            }
        }

        invalidate()  // Request a redraw

        // Schedule the reset after desired interval (e.g., 1 second)
        handler.removeCallbacks(resetFrequencyRunnable)
        handler.postDelayed(resetFrequencyRunnable, 1000)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isAudioInputAvailable && magnitudes.isNotEmpty()) {
            val numberOfBars = 20
            val step = magnitudes.size / numberOfBars
            val barWidth =
                width / (2 * numberOfBars).toFloat() // Each "slot" (bar + gap) is twice the bar width

            for (i in 0 until numberOfBars) {
                val index = i * step
                val magnitude = magnitudes[index]
                val scaleFactor = 10
                val heightMagnitude = min(
                    max(
                        (magnitude / magnitudes.maxOrNull()!! * height *
                                scaleFactor), barWidth
                    ), height.toFloat()
                )

                paint.color = Color.argb(200, 181, 111, 233)

                val x = i * 2 * barWidth
                val yStart = (height - heightMagnitude) / 2 // Starting y-coordinate (top)
                val yEnd = yStart + heightMagnitude // Ending y-coordinate (bottom)
                val radius = barWidth / 2

                canvas.drawRoundRect(
                    x,
                    yStart,
                    x + barWidth,
                    yEnd,
                    radius,
                    radius,
                    paint
                )
            }
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
        floatData = FloatArray(fftSize) { i -> amplitudes.getOrElse(i) { 0 }.toFloat() }

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

    private fun computeDisplayMedianFrequency(frequency: Float): Float {
        synchronized(recentDisplayFrequencies) {
            if (recentDisplayFrequencies.size >= maxDisplayRecentSize) {
                recentDisplayFrequencies.removeAt(0)
            }
            recentDisplayFrequencies.add(frequency)
            return recentDisplayFrequencies.sorted()[recentDisplayFrequencies.size / 2]
        }
    }

    fun setAudioInputAvailable(isAvailable: Boolean) {
        isAudioInputAvailable = isAvailable
        if (!isAvailable) {
            amplitudes = ByteArray(0)
        }
        invalidate()
    }

    fun resetFrequencies() {
        amplitudes = ByteArray(0)
        recentDisplayFrequencies.clear()
        recentMagnitudesAverage.clear()
        dominantFrequency = 0f
        maxFrequency = 0f
        invalidate()  // Request a redraw
    }
}

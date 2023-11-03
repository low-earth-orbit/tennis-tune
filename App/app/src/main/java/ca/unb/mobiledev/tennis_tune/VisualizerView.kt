package ca.unb.mobiledev.tennis_tune

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import org.jtransforms.fft.FloatFFT_1D
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class VisualizerView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val paint = Paint()
    private var isAudioInputAvailable = false

    private val fftSize = 16384
    private val fftSizeHalf = fftSize / 2
    private val noiseThresholdMultiplier = 1.5
    private var floatData = FloatArray(fftSize)
    private var fft = FloatFFT_1D(fftSize.toLong())
    private var magnitudes = FloatArray(fftSizeHalf)
    private var detectedFrequency: Float = 0f
    private var amplitudes = ByteArray(0)

    private val sampleRate = 44100
    private var displayFrequency: Float = 0f
    private var frequencyWindow = mutableListOf<Float>()
    private val frequencyWindowSize =
        60  // Use a rolling window of frequencies

    private var recentMagnitudesAverage = mutableListOf<Float>()
    private val maxMagnitudeAverageSize = fftSizeHalf  // For calculating background
    // noise

    interface OnDisplayFrequencyChangeListener {
        fun onDisplayFrequencyChange(frequency: Float)
    }

    var displayFrequencyListener: OnDisplayFrequencyChangeListener? = null

    init {
        paint.color = Color.argb(200, 181, 111, 233)
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
    }

    fun updateVisualizer(newAmplitudes: ByteArray) {
        // Accumulate audio amplitudes samples until we have enough for an FFT
        amplitudes += newAmplitudes

        // Assuming a 50% overlap for simplicity
        val combinedSize = min(fftSize, amplitudes.size)
        val combinedAmplitudes = amplitudes.sliceArray(0 until combinedSize)

        // Store the half of the current amplitudes for use in the next call
        amplitudes =
            amplitudes.sliceArray(newAmplitudes.size until amplitudes.size)

        // Compute FFT
        computeFFT(combinedAmplitudes)

        // Apply a thresholding method to filter out background noise
        // Compute the rolling average of magnitudes
        val averageMagnitude = magnitudes.average().toFloat()
        if (recentMagnitudesAverage.size >= maxMagnitudeAverageSize) {
            recentMagnitudesAverage.removeAt(0)
        }
        recentMagnitudesAverage.add(averageMagnitude)

        val noiseThreshold = recentMagnitudesAverage.average() * noiseThresholdMultiplier

        // Find the index with the peak amplitude after FFT that's above the noise threshold
        val peakIndex = magnitudes.indices.filter { magnitudes[it] > noiseThreshold }
            .maxByOrNull { magnitudes[it] } ?: -1

        if (peakIndex != -1) {
            detectedFrequency = (peakIndex * sampleRate / (2 * magnitudes.size)).toFloat()

            // Apply a frequency range filter
            if (detectedFrequency in 420f..770f) {
                val displayFrequency = computeDisplayFrequency(detectedFrequency)
                displayFrequencyListener?.onDisplayFrequencyChange(displayFrequency)
            }
        }
        invalidate()  // Request a redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (isAudioInputAvailable && magnitudes.isNotEmpty()) {
            val numberOfBars = 20
            val step = magnitudes.size / numberOfBars
            val barWidth =
                width / (2 * numberOfBars).toFloat() // Each "slot" (bar + gap) is twice the bar width

            val maxMagnitude = magnitudes.maxOrNull() ?: 1f

            for (i in 0 until numberOfBars) {
                // Sum the magnitudes for the current step
                var sumMagnitude = 0f
                for (j in i * step until (i + 1) * step) {
                    sumMagnitude += magnitudes[j]
                }
                sumMagnitude /= step

                val heightMagnitude = min(
                    max(
                        (sumMagnitude / maxMagnitude * height * 10), barWidth
                    ), height.toFloat()
                )

                val x = i * 2 * barWidth
                val yStart = (height - heightMagnitude) / 2 // Starting y-coordinate (top)
                val yEnd = yStart + heightMagnitude // Ending y-coordinate (bottom)
                val radius = barWidth / 2

                // Color gradient to bars/rectangles
                val shader = LinearGradient(
                    0f,
                    0f,
                    0f,
                    height.toFloat(),
                    Color.parseColor("#79528A"),
                    Color.parseColor("#E099FF"),
                    Shader.TileMode.CLAMP
                )
                paint.shader = shader

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

    private fun hammingWindow(data: FloatArray) {
        val n = data.size
        for (i in 0 until n) {
            data[i] *= (0.54 - 0.46 * cos(2 * Math.PI * i / (n - 1))).toFloat()
        }
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
            magnitudes[i] = sqrt(real * real + imaginary * imaginary)
        }
    }

    private fun computeDisplayFrequency(frequency: Float): Float {
        synchronized(frequencyWindow) {
            if (frequencyWindow.size >= frequencyWindowSize) {
                frequencyWindow.removeAt(0)
            }
            frequencyWindow.add(frequency)
            return frequencyWindow.sorted()[frequencyWindow.size / 2]
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
        frequencyWindow.clear()
        recentMagnitudesAverage.clear()
        displayFrequency = 0f
        detectedFrequency = 0f
        invalidate()  // Request a redraw
    }
}

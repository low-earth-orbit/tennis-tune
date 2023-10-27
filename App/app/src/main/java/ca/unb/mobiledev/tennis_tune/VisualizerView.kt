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
import kotlin.math.abs

class VisualizerView : View {
    private val paint = Paint()
    private var amplitudes: ByteArray = ByteArray(0)
    private var isAudioInputAvailable = false
    private val fftSize = 1344
    private val fftSizeHalf = fftSize / 2
    private var floatData = FloatArray(fftSize)
    private var fft = FloatFFT_1D(fftSize.toLong())
    private var magnitudes = FloatArray(fftSizeHalf)

    private var maxFrequency: Float = 0f
    private val handler = Handler(Looper.getMainLooper())
    private val resetFrequencyRunnable = Runnable {
        maxFrequency = 0f
        invalidate()
    }
    private val sampleRate =
        44100  // For example, typical CD quality audio uses a sample rate of 44.1 kHz
    private var dominantFrequency: Float = 0f
    private var prevMagnitudes = FloatArray(fftSizeHalf) // Holds the previous frame's magnitudes
    private var recentDisplayFrequencies = mutableListOf<Float>()
    private val maxDisplayRecentSize =
        30  // Use a rolling window of the last 30 frequencies. Adjust as needed

    private var recentMagnitudesAverage = mutableListOf<Float>()
    private val maxMagnitudeAverageSize = 20  // Using 20 as an example, adjust as needed

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
//        Log.d("VisualizerView", "Incoming audio data size: ${newAmplitudes.size}")
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
            .toFloat() * 2.0  // Using 1 as a multiplier, adjust as needed
//        Log.d("VisualizerView", "noiseThreshold: $noiseThreshold")

        // Find the index with the maximum amplitude after FFT that's above the noise threshold
        val maxIndex = magnitudes.indices.filter { magnitudes[it] > noiseThreshold }
            .maxByOrNull { magnitudes[it] } ?: -1

        if (maxIndex != -1) {
            maxFrequency = (maxIndex * sampleRate / (2 * magnitudes.size)).toFloat()
//            Log.d("VisualizerView", "maxFrequency: $maxFrequency")

            // Apply a frequency range to filter out background noise
            if (maxFrequency in 420f..770f) {
                // Use median frequency measured to enhance the reliability of measurement
                // Median is less sensitive to outliers than mean
                dominantFrequency = computeDisplayMedianFrequency(maxFrequency)
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
            val numberOfBars = 32
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
        floatData = FloatArray(fftSize) { i -> amplitudes.getOrElse(i) { 0 }.toFloat() }

        // Autocorrelation-based filtering
        filterBasedOnAutocorrelation(floatData)

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

    private fun autocorrelation(data: FloatArray): FloatArray {
        val result = FloatArray(data.size)
        for (lag in data.indices) {
            var sum = 0.0f
            for (i in 0 until data.size - lag) {
                sum += data[i] * data[i + lag]
            }
            result[lag] = sum
        }
        return result
    }

    private fun findPeaks(data: FloatArray): List<Int> {
        val peaks = mutableListOf<Int>()
        for (i in 1 until data.size - 1) {
            if (data[i] > data[i - 1] && data[i] > data[i + 1]) {
                peaks.add(i)
            }
        }
        return peaks
    }

    private fun filterBasedOnAutocorrelation(amplitudes: FloatArray): FloatArray {
        val autoCorr = autocorrelation(amplitudes)
        val peaks = findPeaks(autoCorr)

        val percentile = 95f
        val peakThreshold = computePercentileThreshold(autoCorr, percentile)

        for (i in amplitudes.indices) {
            if (peaks.any { peak -> abs(peak - i) < 5 && autoCorr[peak] > peakThreshold }) {
                // Keep the amplitude as it is if it's close to a peak and the peak is above threshold
            } else {
                amplitudes[i] = 0f
            }
        }
        return amplitudes
    }

    fun computePercentileThreshold(data: FloatArray, percentile: Float): Float {
        // Ensure the input percentile is between 0 and 100.
        if (percentile <= 0f || percentile >= 100f) {
            throw IllegalArgumentException("Percentile must be between 0 and 100")
        }

        // Sort the data.
        val sortedData = data.sorted()

        // Compute the index position for the desired percentile.
        val index = (percentile / 100 * (sortedData.size - 1)).toInt()

        return sortedData[index]
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

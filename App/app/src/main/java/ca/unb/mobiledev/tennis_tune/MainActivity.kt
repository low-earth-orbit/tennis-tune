package ca.unb.mobiledev.tennis_tune

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import ca.unb.mobiledev.tennis_tune.databinding.HomePageBinding
import kotlinx.coroutines.*
import kotlin.math.pow

class MainActivity : AppCompatActivity(), VisualizerView.OnDisplayFrequencyChangeListener {

    private lateinit var binding: HomePageBinding
    private val recordAudioPermission = 1
    private val visualizerHeightDip = 50f
    private var mVisualizer: Visualizer? = null
    private var mLinearLayout: LinearLayout? = null
    private var mVisualizerView: VisualizerView? = null
    private var frequencyTextView: TextView? = null
    private val job = Job()
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = HomePageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.topBarHome)
        supportActionBar?.title = "Home"

        frequencyTextView = findViewById(R.id.frequencyTextView)
        setupVisualizerFxAndUI()
        mVisualizerView?.displayFrequencyListener = this

        checkRecordAudioPermission()

        resetButton = findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            mVisualizerView?.resetFrequencies()
            frequencyTextView?.text = "Detecting..."
        }

        val setUpButton = Button(this).apply {
            text = "Set Up"
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setOnClickListener {
                val intent = Intent(this@MainActivity, SettingsActivity::class.java)
                startActivity(intent)
            }
        }
        findViewById<LinearLayout>(R.id.bottomMenu).addView(setUpButton)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }

    private fun setupVisualizerFxAndUI() {
        mVisualizerView = VisualizerView(this)
        mVisualizerView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (this.visualizerHeightDip * resources.displayMetrics.density).toInt()
        )
        mLinearLayout = findViewById(R.id.my_visualizer_container)
        mLinearLayout?.addView(mVisualizerView)
    }

    override fun onPause() {
        super.onPause()
        if (isFinishing) {
            mVisualizer?.release()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        val audioSource = MediaRecorder.AudioSource.MIC
        val sampleRateInHz = 44100
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_8BIT
        val bufferSizeInBytes =
            AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat)
        val audioRecord =
            AudioRecord(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes)
        val buffer = ByteArray(bufferSizeInBytes)
        val scope = CoroutineScope(Dispatchers.IO + job)

        scope.launch {
            audioRecord.startRecording()
            mVisualizerView?.setAudioInputAvailable(true)

            while (isActive) {
                val readSize = audioRecord.read(buffer, 0, buffer.size, AudioRecord.READ_BLOCKING)
                mVisualizerView?.updateVisualizer(buffer.sliceArray(0 until readSize))
            }
            audioRecord.stop()
            mVisualizerView?.setAudioInputAvailable(false)
        }
    }

    private fun checkRecordAudioPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permission was denied previously and the user checked "Don't ask again"
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.RECORD_AUDIO
                )
            ) {
                // Show an explanation to the user
                Toast.makeText(
                    this,
                    "We need audio permission for string tension measurement",
                    Toast.LENGTH_LONG
                ).show()
            }

            // Request the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                recordAudioPermission
            )
        } else {
            // Permission has already been granted, proceed with audio capture
            startAudioCapture()
        }
    }

    override fun onDisplayFrequencyChange(frequency: Float) {
        Log.d("MainActivity", "Display Frequency: $frequency")

        val stringMassDensity = 0.0015
        val racquetHeadSize = 0.0645

        val tension = frequencyToTension(frequency, racquetHeadSize, stringMassDensity)

        runOnUiThread {
            frequencyTextView?.text = buildString {
                append(
                    "Frequency: ${"%.0f".format(frequency)} Hz\nTension: ${
                        "%.1f"
                            .format(tension)
                    } lb"
                )
            }
        }
    }

    private fun frequencyToTension(
        frequency: Float,
        racquetHeadSize: Double,
        stringMassDensity: Double
    ): Double {
        val toSingleStringFreqFactor = 1.0121457 // Conversion factor from elliptical membrane
        // to square area tension. The conversion logic is based on square area assumption
        val toMachineTensionFactor = 1.470588235 // Machine
        // tension is the pull tension. Actual tension is about 32% lower than machine tension,
        // immediately after
        // strung. Despite this fact, to avoid confusion, display tension in the app is to
        // approximate the machine tension, thus requiring this conversion factor.
        val newtonToLbFactor = 0.2248089431
        val tensionNewton = 4 * racquetHeadSize * stringMassDensity * (frequency *
                toSingleStringFreqFactor).pow(2)
        return tensionNewton * toMachineTensionFactor * newtonToLbFactor
    }
}

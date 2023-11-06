package ca.unb.mobiledev.tennis_tune

import android.Manifest
import android.annotation.SuppressLint
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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import ca.unb.mobiledev.tennis_tune.databinding.ActivityMainBinding
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.*
import kotlin.math.pow

class MainActivity : AppCompatActivity(), VisualizerView.OnDisplayFrequencyChangeListener {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private val MY_PERMISSIONS_RECORD_AUDIO = 1
    private val VISUALIZER_HEIGHT_DIP = 50f
    private var mVisualizer: Visualizer? = null
    private var mLinearLayout: LinearLayout? = null
    private var mVisualizerView: VisualizerView? = null
    private var frequencyTextView: TextView? = null
    private val job = Job()
    private lateinit var resetButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        frequencyTextView = findViewById(R.id.frequencyTextView)

        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        setupVisualizerFxAndUI()
        mVisualizerView?.displayFrequencyListener = this

        checkRecordAudioPermission()

        resetButton = findViewById(R.id.resetButton)
        resetButton.setOnClickListener {
            mVisualizerView?.resetFrequencies()
            frequencyTextView?.text = "Detecting..."
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun setupVisualizerFxAndUI() {
        mVisualizerView = VisualizerView(this)
        mVisualizerView!!.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (this.VISUALIZER_HEIGHT_DIP * resources.displayMetrics.density).toInt()
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
                MY_PERMISSIONS_RECORD_AUDIO
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

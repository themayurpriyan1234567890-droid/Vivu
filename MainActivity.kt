package com.example.vivuvizualizer

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.audiofx.Visualizer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

class MainActivity : ComponentActivity() {
    private var player: ExoPlayer? = null
    private var visualizer: Visualizer? = null
    private lateinit var visualizerView: SpectrumView
    private lateinit var playerView: PlayerView
    private lateinit var status: TextView
    private var selectedUri: Uri? = null
    private var audioSessionId: Int = 0

    private val pickAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { openMedia(it, isVideo = false) }
    }
    private val pickVideo = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { openMedia(it, isVideo = true) }
    }
    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) attachVisualizer()
            else toast("Microphone permission is needed to analyse playback audio.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildScreen()
        ensureAudioPermission()
    }

    private fun buildScreen() {
        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(10, 12, 20))
            setPadding(dp(16), dp(18), dp(16), dp(16))
        }

        val heading = TextView(this).apply {
            text = "VIVU VISUALIZER"
            textSize = 22f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        }
        page.addView(heading, LinearLayout.LayoutParams(-1, dp(52)))

        status = TextView(this).apply {
            text = "Choose an audio track or a video"
            textSize = 13f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
        }
        page.addView(status, LinearLayout.LayoutParams(-1, dp(36)))

        val preview = android.widget.FrameLayout(this).apply {
            setBackgroundColor(Color.rgb(20, 24, 36))
        }
        playerView = PlayerView(this).apply {
            useController = true
            setShutterBackgroundColor(Color.TRANSPARENT)
            setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        }
        preview.addView(playerView, android.widget.FrameLayout.LayoutParams(-1, -1))
        visualizerView = SpectrumView(this)
        preview.addView(
            visualizerView,
            android.widget.FrameLayout.LayoutParams(-1, -1)
        )
        page.addView(
            preview,
            LinearLayout.LayoutParams(-1, 0, 1f).apply {
                topMargin = dp(12)
                bottomMargin = dp(14)
            }
        )

        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        buttons.addView(actionButton("ADD AUDIO") { pickAudio.launch(arrayOf("audio/*")) })
        buttons.addView(actionButton("ADD VIDEO") { pickVideo.launch(arrayOf("video/*")) })
        page.addView(buttons, LinearLayout.LayoutParams(-1, dp(58)))

        val note = TextView(this).apply {
            text = "Live audio-reactive preview • Audio and video playback"
            textSize = 11f
            setTextColor(Color.GRAY)
            gravity = Gravity.CENTER
        }
        page.addView(note, LinearLayout.LayoutParams(-1, dp(34)))
        setContentView(page)
    }

    private fun actionButton(label: String, click: () -> Unit): Button =
        Button(this).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.rgb(90, 55, 220))
            setOnClickListener { click() }
            layoutParams = LinearLayout.LayoutParams(0, dp(50), 1f).apply {
                marginStart = dp(5)
                marginEnd = dp(5)
            }
        }

    private fun openMedia(uri: Uri, isVideo: Boolean) {
        selectedUri = uri
        val exo = player ?: ExoPlayer.Builder(this).build().also {
            player = it
            playerView.player = it
            it.addListener(object : Player.Listener {
                override fun onAudioSessionIdChanged(audioSessionId: Int) {
                    this@MainActivity.audioSessionId = audioSessionId
                    attachVisualizer()
                }
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_READY) attachVisualizer()
                }
            })
        }

        stopVisualizer()
        visualizerView.setData(ByteArray(0))
        playerView.visibility = if (isVideo) View.VISIBLE else View.INVISIBLE
        exo.setMediaItem(MediaItem.fromUri(uri))
        exo.prepare()
        exo.playWhenReady = true
        status.text = if (isVideo) "Video selected • visualizer reacts to its audio" else "Audio selected • live spectrum"
        Toast.makeText(this, if (isVideo) "Video loaded" else "Audio loaded", Toast.LENGTH_SHORT).show()
    }

    private fun ensureAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED) return
        requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun attachVisualizer() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ensureAudioPermission()
            return
        }
        val session = player?.audioSessionId ?: audioSessionId
        if (session <= 0) return
        stopVisualizer()
        try {
            visualizer = Visualizer(session).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                    override fun onWaveFormDataCapture(v: Visualizer?, waveform: ByteArray?, samplingRate: Int) {
                        if (waveform != null) visualizerView.setData(waveform)
                    }
                    override fun onFftDataCapture(v: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                        if (fft != null) visualizerView.setData(fft, isFft = true)
                    }
                }, Visualizer.getMaxCaptureRate() / 2, true, true)
                enabled = true
            }
        } catch (_: Exception) {
            status.text = "Audio visualizer unavailable on this device"
        }
    }

    private fun stopVisualizer() {
        try {
            visualizer?.enabled = false
            visualizer?.release()
        } catch (_: Exception) { }
        visualizer = null
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

    override fun onStop() {
        super.onStop()
        stopVisualizer()
    }

    override fun onDestroy() {
        stopVisualizer()
        player?.release()
        player = null
        super.onDestroy()
    }
}

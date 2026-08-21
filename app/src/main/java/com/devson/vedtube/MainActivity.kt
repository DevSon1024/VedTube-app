package com.devson.vedtube

import android.app.PictureInPictureParams
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtube.core.ui.theme.VedTubeTheme
import com.devson.vedtube.feature.home.HomeViewModel
import com.devson.vedtube.navigation.VedTubeNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private var isInPipMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        processIntent(intent)

        setContent {
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()
            val playerState by homeViewModel.vedPlayer.playerState.collectAsStateWithLifecycle()

            // Update PiP parameters whenever playback status changes
            LaunchedEffect(playerState.isPlaying) {
                updatePipParams(playerState.isPlaying)
            }

            VedTubeTheme(
                appThemeConfig = uiState.themeSettings.themeConfig,
                dynamicColor = uiState.themeSettings.dynamicColor
            ) {
                VedTubeNavHost(
                    homeViewModel = homeViewModel,
                    isInPipMode = isInPipMode,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipMode = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (supportsPictureInPicture()) {
            val isPlaying = homeViewModel.vedPlayer.playerState.value.isPlaying
            if (isPlaying && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                try {
                    enterPictureInPictureMode(params)
                } catch (e: Exception) {
                    // Ignore if PiP could not be entered
                }
            }
        }
    }

    private fun updatePipParams(isPlaying: Boolean) {
        if (supportsPictureInPicture() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val paramsBuilder = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                paramsBuilder.setAutoEnterEnabled(isPlaying)
            }

            try {
                setPictureInPictureParams(paramsBuilder.build())
            } catch (e: Exception) {
                // Ignore if device does not support setting params
            }
        }
    }

    private fun supportsPictureInPicture(): Boolean {
        return packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        processIntent(intent)
    }

    private fun processIntent(intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
                if (!sharedText.isNullOrBlank()) {
                    homeViewModel.handleIncomingIntent(sharedText)
                }
            }
            Intent.ACTION_VIEW -> {
                val dataString = intent.dataString
                if (!dataString.isNullOrBlank()) {
                    homeViewModel.handleIncomingIntent(dataString)
                }
            }
        }
    }
}
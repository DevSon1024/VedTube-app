package com.devson.vedtube

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.devson.vedtube.core.ui.theme.VedTubeTheme
import com.devson.vedtube.feature.home.HomeViewModel
import com.devson.vedtube.navigation.VedTubeNavHost
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        processIntent(intent)

        setContent {
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

            VedTubeTheme(
                appThemeConfig = uiState.themeSettings.themeConfig,
                dynamicColor = uiState.themeSettings.dynamicColor
            ) {
                VedTubeNavHost(
                    homeViewModel = homeViewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
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
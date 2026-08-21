package com.devson.vedtube

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

        setContent {
            val uiState by homeViewModel.uiState.collectAsStateWithLifecycle()

            VedTubeTheme(
                appThemeConfig = uiState.themeSettings.themeConfig,
                dynamicColor = uiState.themeSettings.dynamicColor
            ) {
                VedTubeNavHost(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
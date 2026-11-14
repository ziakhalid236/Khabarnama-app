package com.example

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.NewsViewModel
import com.example.ui.UrduNewsMainScreen
import com.example.ui.theme.UrduNewsTheme
import com.example.util.NewsNotificationHelper
import com.example.util.configureAppCheck

class MainActivity : ComponentActivity() {

    private val viewModel: NewsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        com.example.util.FirebaseInitHelper.registerContext(this)
        configureAppCheck(this, intent)
        com.example.data.remote.RemoteConfigManager.initialize(this)
        NewsNotificationHelper.ensureNotificationChannel(this)
        handleNotificationIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            val isSearchOpen by viewModel.isSearchOpen.collectAsStateWithLifecycle()
            val selectedArticle by viewModel.selectedArticleForDetail.collectAsStateWithLifecycle()
            val selectedVideoClip by viewModel.selectedVideoClip.collectAsStateWithLifecycle()
            val activeImageSource by viewModel.activeImageSourceArticle.collectAsStateWithLifecycle()
            val isShowingLanguageDialog by viewModel.isShowingLanguageDialog.collectAsStateWithLifecycle()

            // Back button handlers
            BackHandler(
                enabled = isShowingLanguageDialog || isSearchOpen || selectedArticle != null || selectedVideoClip != null || activeImageSource != null
            ) {
                when {
                    isShowingLanguageDialog -> viewModel.showLanguageDialog(false)
                    activeImageSource != null -> viewModel.closeImageSourceSheet()
                    selectedVideoClip != null -> viewModel.closeVideoClip()
                    selectedArticle != null -> viewModel.closeArticleDetail()
                    isSearchOpen -> viewModel.toggleSearchOpen(false)
                }
            }

            UrduNewsTheme {
                UrduNewsMainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val articleId = intent?.getStringExtra(NewsNotificationHelper.EXTRA_NOTIFICATION_ARTICLE_ID)
        if (!articleId.isNullOrBlank()) {
            viewModel.openArticleById(articleId)
        }
    }
}

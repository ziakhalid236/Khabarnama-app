package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.AppLanguage
import com.example.model.NewsVideoClip
import com.example.ui.theme.CrimsonBreaking
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldTint
import com.example.ui.theme.InkPrimary
import com.example.ui.theme.Inter
import com.example.ui.theme.NotoNaskhArabic
import com.example.ui.theme.NotoNastaliqUrdu
import com.example.ui.theme.PlayfairDisplay
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SoftGrey
import com.example.ui.theme.WarmBorder
import com.example.ui.theme.WarmDivider
import com.example.ui.theme.WarmOffWhite
import com.example.util.BrowserUtil
import com.example.util.PrayerTimesUtil

private val SAFE_YOUTUBE_ID_REGEX = Regex("[^A-Za-z0-9_-]")

fun buildYouTubeIframeHtml(videoId: String, muted: Boolean): String {
    val safeVideoId = videoId.replace(SAFE_YOUTUBE_ID_REGEX, "").take(32)
    val muteParam = if (muted) 1 else 0
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    background-color: #050506;
                    overflow: hidden;
                }
                iframe {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    border: 0;
                }
            </style>
        </head>
        <body>
            <iframe
                src="https://www.youtube.com/embed/$safeVideoId?autoplay=1&mute=$muteParam&playsinline=1&rel=0&modestbranding=1&controls=1&fs=1&enablejsapi=1&origin=https://www.youtube.com"
                title="Video player"
                frameborder="0"
                referrerpolicy="strict-origin-when-cross-origin"
                allow="autoplay; encrypted-media; accelerometer; gyroscope; picture-in-picture; fullscreen"
                allowfullscreen>
            </iframe>
        </body>
        </html>
    """.trimIndent()
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeEmbedPlayer(
    videoId: String,
    muted: Boolean,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeId = remember(videoId) { videoId.replace(SAFE_YOUTUBE_ID_REGEX, "") }
    val webViewHolder = remember(safeId) { arrayOfNulls<WebView>(1) }
    val lastMutedHolder = remember(safeId) { arrayOfNulls<Boolean>(1) }

    DisposableEffect(safeId) {
        onDispose {
            webViewHolder[0]?.let { webView ->
                runCatching {
                    webView.stopLoading()
                    webView.loadUrl("about:blank")
                    webView.onPause()
                    webView.removeAllViews()
                    webView.destroy()
                }
            }
            webViewHolder[0] = null
        }
    }

    AndroidView(
        factory = { ctx ->
            WebView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundColor(AndroidColor.rgb(5, 5, 6))
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.javaScriptCanOpenWindowsAutomatically = false
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        if (request?.isForMainFrame != true) {
                            return false
                        }
                        val targetUrl = request.url?.toString().orEmpty()
                        if (targetUrl == "about:blank" ||
                            targetUrl.startsWith("https://www.youtube.com/embed/") ||
                            targetUrl.startsWith("https://www.youtube-nocookie.com/embed/")
                        ) {
                            return false
                        }
                        if (request.hasGesture() &&
                            (targetUrl.startsWith("https://") || targetUrl.startsWith("http://"))
                        ) {
                            onOpenExternal()
                        }
                        return true
                    }
                }
                webViewHolder[0] = this
                lastMutedHolder[0] = muted
                loadDataWithBaseURL(
                    "https://www.youtube.com",
                    buildYouTubeIframeHtml(safeId, muted),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        },
        update = { webView ->
            if (lastMutedHolder[0] != muted) {
                lastMutedHolder[0] = muted
                webView.loadDataWithBaseURL(
                    "https://www.youtube.com",
                    buildYouTubeIframeHtml(safeId, muted),
                    "text/html",
                    "utf-8",
                    null
                )
            }
        },
        modifier = modifier
    )
}

@Composable
fun VideoPlayerDialog(
    clip: NewsVideoClip,
    currentLanguage: AppLanguage,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasTranslated = currentLanguage != AppLanguage.ENGLISH && !clip.translatedTitle.isNullOrBlank()
    val effectiveLang = if (hasTranslated) currentLanguage else AppLanguage.ENGLISH
    val displayTitle = clip.getDisplayTitle(effectiveLang)
    val isUrdu = effectiveLang == AppLanguage.URDU
    val isArabic = effectiveLang == AppLanguage.ARABIC
    val isRtl = isUrdu || isArabic

    val headlineFont = when {
        isUrdu -> NotoNastaliqUrdu
        isArabic -> NotoNaskhArabic
        else -> PlayfairDisplay
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        BackHandler(onBack = onDismiss)

        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Scaffold(
                topBar = {
                    Surface(
                        color = PureWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(DeepEmerald),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified broadcaster",
                                            tint = PureWhite,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = clip.channelName,
                                            fontFamily = Inter,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = InkPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Video Dispatch • ${PrayerTimesUtil.formatRelativeTime(clip.publishedAt, AppLanguage.ENGLISH)}",
                                            fontFamily = Inter,
                                            fontSize = 11.5.sp,
                                            color = SoftGrey,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            val shareText = "$displayTitle\n\nWatch from ${clip.channelName}: ${clip.watchUrl}"
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, displayTitle)
                                                putExtra(Intent.EXTRA_TEXT, shareText)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share video dispatch"))
                                        },
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("video_share_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share video clip",
                                            tint = InkPrimary
                                        )
                                    }

                                    IconButton(
                                        onClick = onDismiss,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .testTag("close_video_player_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close video player",
                                            tint = InkPrimary
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(thickness = 1.dp, color = WarmDivider)
                        }
                    }
                },
                containerColor = WarmOffWhite,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("video_player_dialog")
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                ) {
                    // Matte Obsidian Cinema Video Player Frame (16:9)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color(0xFF050506))
                    ) {
                        if (clip.videoId.isNotBlank()) {
                            YouTubeEmbedPlayer(
                                videoId = clip.videoId,
                                muted = false,
                                onOpenExternal = { BrowserUtil.openCustomTab(context, clip.watchUrl) },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            BrandedSourcePlaceholder(
                                sourceName = clip.channelName,
                                category = clip.category
                            )
                        }
                    }

                    // Editorial Details & Source Attribution
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepEmerald)
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "VIDEO • ${clip.category.englishLabel.uppercase()}",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp,
                                    letterSpacing = 0.5.sp,
                                    color = PureWhite
                                )
                            }

                            if (clip.isLiveBadge) {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CrimsonBreaking)
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(PureWhite)
                                    )
                                    Text(
                                        text = "LIVE STREAM",
                                        fontFamily = Inter,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.5.sp,
                                        letterSpacing = 0.6.sp,
                                        color = PureWhite
                                    )
                                }
                            }
                        }

                        CompositionLocalProvider(
                            LocalLayoutDirection provides (if (isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = displayTitle,
                                    style = TextStyle(
                                        fontFamily = headlineFont,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = if (isUrdu) 21.sp else if (isArabic) 19.sp else 20.sp,
                                        lineHeight = if (isUrdu) 38.sp else if (isArabic) 31.sp else 27.sp,
                                        color = InkPrimary,
                                        textAlign = TextAlign.Start,
                                        platformStyle = PlatformTextStyle(includeFontPadding = true)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (clip.summary.isNotBlank() && !clip.summary.equals(clip.title, ignoreCase = true)) {
                                    Text(
                                        text = clip.summary,
                                        style = TextStyle(
                                            fontFamily = Inter,
                                            fontSize = 14.sp,
                                            lineHeight = 21.sp,
                                            color = InkPrimary.copy(alpha = 0.85f),
                                            textAlign = TextAlign.Start
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        // Official Source Attribution Card
                        Surface(
                            color = PureWhite,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, WarmBorder, RoundedCornerShape(14.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(18.dp)
                                )
                                Text(
                                    text = "Streamed directly from ${clip.channelName}'s official broadcast. Khabarnama does not re-upload third-party footage. If a broadcaster restricts embedded playback on mobile, tap below to open the stream directly.",
                                    fontFamily = Inter,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = SoftGrey,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Action Buttons (48dp+)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = { BrowserUtil.openCustomTab(context, clip.watchUrl) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = PureWhite
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .testTag("watch_original_channel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Watch on ${clip.channelName}",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            OutlinedButton(
                                onClick = onDismiss,
                                border = BorderStroke(1.dp, WarmBorder),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("dismiss_video_button")
                            ) {
                                Text(
                                    text = "Close",
                                    fontFamily = Inter,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = InkPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

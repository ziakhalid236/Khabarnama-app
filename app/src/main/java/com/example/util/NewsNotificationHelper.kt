package com.example.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.remote.RssRemoteNewsSource
import com.example.model.AppLanguage
import com.example.model.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class NotificationFocus(val label: String, val subtitle: String) {
    ALL_PRIORITY("All Priority", "War, Security, Palestine & Global"),
    SECURITY_WAR("War & Security", "Conflict, Defense, OSINT & Tensions"),
    PALESTINE("Palestine", "Gaza, West Bank & Regional"),
    ISLAMIC_SCHOLARS("Scholars & Ummah", "Arab & Ajam Scholars, Haramain")
}

object NewsNotificationHelper {

    const val CHANNEL_ID = "khabarnama_breaking_news"
    private const val CHANNEL_NAME = "Breaking & Priority Dispatches"
    private const val CHANNEL_DESC = "War, security, Palestine, and scholarly Islamic alerts with photo previews"
    const val EXTRA_NOTIFICATION_ARTICLE_ID = "extra_notification_article_id"

    private const val PREFS_NAME = "khabarnama_prefs"
    const val KEY_NOTIFICATIONS_ENABLED = "breaking_notifications_enabled"
    const val KEY_NOTIFICATION_FOCUS = "notification_alert_focus"
    private const val KEY_NOTIFIED_IDS = "notified_article_ids"
    const val KEY_INSTALL_BASELINE_MS = "notification_install_baseline_ms"
    const val KEY_BASELINE_ARTICLE_IDS = "notification_baseline_article_ids"
    private const val MAX_STORED_NOTIFIED_IDS = 80
    private const val MAX_STORED_BASELINE_IDS = 350

    private val imageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(8, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isNotificationsEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true)
    }

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun getNotificationFocus(context: Context): NotificationFocus {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_NOTIFICATION_FOCUS, NotificationFocus.ALL_PRIORITY.name)
        return NotificationFocus.entries.firstOrNull { it.name == raw } ?: NotificationFocus.ALL_PRIORITY
    }

    fun setNotificationFocus(context: Context, focus: NotificationFocus) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_NOTIFICATION_FOCUS, focus.name).apply()
    }

    fun isBaselineEstablished(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_INSTALL_BASELINE_MS, 0L) > 0L
    }

    fun getInstallBaselineMs(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_INSTALL_BASELINE_MS, 0L)
    }

    fun getBaselineArticleIds(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_BASELINE_ARTICLE_IDS, "").orEmpty()
        if (raw.isBlank()) return emptySet()
        return raw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()
    }

    /**
     * Records all initial articles on first launch as the installation baseline so pre-existing
     * RSS stories never flood the Notifications tab or trigger push alerts right after install.
     * Returns true if this call established a brand-new baseline.
     */
    fun establishInstallBaselineIfNeeded(context: Context, initialArticles: List<NewsArticle>): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val existingBaselineMs = prefs.getLong(KEY_INSTALL_BASELINE_MS, 0L)
        if (existingBaselineMs > 0L) {
            return false
        }
        val baselineTimestamp = System.currentTimeMillis()
        val baselineIds = initialArticles
            .map { it.id.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(MAX_STORED_BASELINE_IDS)
        prefs.edit()
            .putLong(KEY_INSTALL_BASELINE_MS, baselineTimestamp)
            .putString(KEY_BASELINE_ARTICLE_IDS, baselineIds.joinToString(","))
            .commit()
        return true
    }

    /**
     * Automatically sends a rich image notification for the newest high-priority breaking story
     * arriving after the install baseline, respecting alert focus and excluding Sports/Business.
     */
    suspend fun notifyLatestBreakingNewsIfNew(
        context: Context,
        articles: List<NewsArticle>,
        language: AppLanguage = AppLanguage.ENGLISH
    ): Boolean = withContext(Dispatchers.IO) {
        if (!isNotificationsEnabled(context) || !hasNotificationPermission(context)) {
            return@withContext false
        }
        if (articles.isEmpty()) return@withContext false

        // On first baseline establishing sync, record existing articles quietly and do not push old news
        if (!isBaselineEstablished(context)) {
            establishInstallBaselineIfNeeded(context, articles)
            return@withContext false
        }

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val baselineMs = prefs.getLong(KEY_INSTALL_BASELINE_MS, 0L)
        val baselineIds = getBaselineArticleIds(context)
        val notifiedRaw = prefs.getString(KEY_NOTIFIED_IDS, "").orEmpty()
        val notifiedSet = notifiedRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        val focus = getNotificationFocus(context)

        // Only notify for genuinely new dispatches published after install baseline & within 12h
        val twelveHoursAgo = System.currentTimeMillis() - (12L * 3600L * 1000L)
        val minFreshTimestamp = maxOf(twelveHoursAgo, baselineMs)
        val eligiblePool = articles.filter { article ->
            article.publishedAt > minFreshTimestamp &&
                article.id !in baselineIds &&
                article.id !in notifiedSet &&
                article.category != com.example.model.NewsCategory.SPORTS &&
                article.category != com.example.model.NewsCategory.BUSINESS
        }

        if (eligiblePool.isEmpty()) return@withContext false

        val focusMatched = when (focus) {
            NotificationFocus.ALL_PRIORITY -> eligiblePool
            NotificationFocus.SECURITY_WAR -> eligiblePool.filter {
                it.category == com.example.model.NewsCategory.SECURITY ||
                    it.category == com.example.model.NewsCategory.PALESTINE ||
                    it.category == com.example.model.NewsCategory.WORLD ||
                    it.category == com.example.model.NewsCategory.PAKISTAN
            }.ifEmpty { eligiblePool }
            NotificationFocus.PALESTINE -> eligiblePool.filter {
                it.category == com.example.model.NewsCategory.PALESTINE
            }.ifEmpty { eligiblePool }
            NotificationFocus.ISLAMIC_SCHOLARS -> eligiblePool.filter {
                it.category == com.example.model.NewsCategory.ISLAMIC
            }.ifEmpty { eligiblePool }
        }

        val ranked = focusMatched.sortedByDescending { article ->
            val catWeight = when (article.category) {
                com.example.model.NewsCategory.SECURITY -> 4L * 3600_000L
                com.example.model.NewsCategory.PALESTINE -> 4L * 3600_000L
                com.example.model.NewsCategory.ISLAMIC -> 3L * 3600_000L
                com.example.model.NewsCategory.WORLD -> 2L * 3600_000L
                com.example.model.NewsCategory.PAKISTAN -> 2L * 3600_000L
                else -> 0L
            }
            val quoteBoost = if (article.hasSocialQuote) 1800_000L else 0L
            article.publishedAt + catWeight + quoteBoost
        }

        // Prefer a fresh priority story that has a valid hero image
        val candidate = ranked.firstOrNull { it.hasValidImage } ?: ranked.first()

        val sent = postRichArticleNotification(context, candidate, language)
        if (sent) {
            val updated = (listOf(candidate.id) + notifiedSet).distinct().take(MAX_STORED_NOTIFIED_IDS)
            prefs.edit().putString(KEY_NOTIFIED_IDS, updated.joinToString(",")).apply()
        }
        sent
    }

    /**
     * Immediately sends a rich BigPictureStyle notification for the given article.
     */
    @SuppressLint("MissingPermission")
    suspend fun postRichArticleNotification(
        context: Context,
        article: NewsArticle,
        language: AppLanguage = AppLanguage.ENGLISH
    ): Boolean = withContext(Dispatchers.IO) {
        if (!hasNotificationPermission(context)) return@withContext false

        return@withContext try {
            ensureNotificationChannel(context)

            val effectiveLang = if (article.hasTranslation(language)) language else AppLanguage.ENGLISH
            val title = article.getTitle(effectiveLang).ifBlank { article.title }
            val summary = article.getSummary(effectiveLang).ifBlank { article.summary }

            val heroBitmap = downloadArticleBitmap(article.imageUrl)
                ?: createBrandedBannerBitmap(article.sourceName, article.category.englishLabel)

            val launchIntent = Intent(context, MainActivity::class.java).apply {
                action = "com.khabarnama.news.OPEN_ARTICLE_${article.id}"
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NOTIFICATION_ARTICLE_ID, article.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                article.id.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val subLabel = when (article.category) {
                com.example.model.NewsCategory.SECURITY,
                com.example.model.NewsCategory.PALESTINE -> "PRIORITY • ${article.category.englishLabel} • ${article.sourceName}"
                com.example.model.NewsCategory.ISLAMIC -> "UMMAH DISPATCH • ${article.sourceName}"
                else -> "Breaking • ${article.sourceName}"
            }

            val bigPictureStyle = NotificationCompat.BigPictureStyle()
                .bigPicture(heroBitmap)
                .bigLargeIcon(null as Bitmap?)
                .setBigContentTitle(title)
                .setSummaryText(summary.take(180))

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_k)
                .setColor(0xFF0D0D0E.toInt())
                .setSubText(subLabel)
                .setContentTitle(title)
                .setContentText(summary)
                .setLargeIcon(heroBitmap)
                .setStyle(bigPictureStyle)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            NotificationManagerCompat.from(context).notify(
                article.id.hashCode(),
                notification
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun downloadArticleBitmap(imageUrl: String): Bitmap? {
        if (imageUrl.isBlank() || !imageUrl.startsWith("http")) return null
        return try {
            val request = Request.Builder()
                .url(imageUrl)
                .header("User-Agent", RssRemoteNewsSource.USER_AGENT)
                .build()
            imageHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body ?: return null
                val contentLength = body.contentLength()
                if (contentLength > 4L * 1024L * 1024L) return null
                val bytes = body.bytes()
                if (bytes.isEmpty() || bytes.size > 4 * 1024 * 1024) return null

                // Decode with bounds check to keep notification bitmap memory-safe (~1024x576)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
                var sampleSize = 1
                while (boundsOpts.outWidth / sampleSize > 1024 || boundsOpts.outHeight / sampleSize > 576) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            }
        } catch (_: Throwable) {
            null
        }
    }

    internal fun createBrandedBannerBitmap(sourceName: String, categoryLabel: String): Bitmap {
        val width = 960
        val height = 540
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Matte Obsidian Black to Charcoal editorial gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#08080A"),
                    Color.parseColor("#111113"),
                    Color.parseColor("#1C1C1F")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Crisp white editorial top rule & subtle geometric wireframe rings
        val rulePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 10f, rulePaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(38, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 3.5f
        }
        canvas.drawCircle(width * 0.85f, height * 0.25f, 180f, ringPaint)
        canvas.drawCircle(width * 0.15f, height * 0.8f, 140f, ringPaint)

        // Brand title & source text
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 54f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(sourceName, width / 2f, height / 2f - 10f, titlePaint)

        val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D4D4D8")
            textSize = 28f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("KHABARNAMA • ${categoryLabel.uppercase()} DISPATCH", width / 2f, height / 2f + 50f, subPaint)

        return bitmap
    }
}

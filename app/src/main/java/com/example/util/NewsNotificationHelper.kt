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

object NewsNotificationHelper {

    const val CHANNEL_ID = "khabarnama_breaking_news"
    private const val CHANNEL_NAME = "Breaking & Featured News"
    private const val CHANNEL_DESC = "Important breaking news and top dispatches with photo previews"
    const val EXTRA_NOTIFICATION_ARTICLE_ID = "extra_notification_article_id"

    private const val PREFS_NAME = "khabarnama_prefs"
    const val KEY_NOTIFICATIONS_ENABLED = "breaking_notifications_enabled"
    private const val KEY_NOTIFIED_IDS = "notified_article_ids"
    private const val MAX_STORED_NOTIFIED_IDS = 50

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

    /**
     * Automatically sends a rich image notification for the newest breaking/top story
     * if it hasn't been notified before.
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

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val notifiedRaw = prefs.getString(KEY_NOTIFIED_IDS, "").orEmpty()
        val notifiedSet = notifiedRaw.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toMutableList()

        // Prefer a story that has a valid image and hasn't been notified yet
        val candidate = articles.firstOrNull { it.hasValidImage && it.id !in notifiedSet }
            ?: articles.firstOrNull { it.id !in notifiedSet }
            ?: return@withContext false

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
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NOTIFICATION_ARTICLE_ID, article.id)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                article.id.hashCode(),
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val bigPictureStyle = NotificationCompat.BigPictureStyle()
                .bigPicture(heroBitmap)
                .bigLargeIcon(null as Bitmap?)
                .setBigContentTitle(title)
                .setSummaryText("${article.sourceName} • ${article.category.englishLabel}")

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_monochrome)
                .setColor(0xFF0F5C4D.toInt())
                .setSubText("Breaking • ${article.sourceName}")
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
                val bytes = response.body?.bytes() ?: return null
                if (bytes.isEmpty()) return null

                // Decode with bounds check to keep notification bitmap memory-safe (~1024x576)
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
                var sampleSize = 1
                while (boundsOpts.outWidth / sampleSize > 1280 || boundsOpts.outHeight / sampleSize > 720) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
            }
        } catch (_: Exception) {
            null
        }
    }

    internal fun createBrandedBannerBitmap(sourceName: String, categoryLabel: String): Bitmap {
        val width = 960
        val height = 540
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Deep Emerald to Dark Emerald gradient background
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#0A3E34"),
                    Color.parseColor("#0F5C4D"),
                    Color.parseColor("#186B5A")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle gold accent ring & top bar
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B8913B")
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 12f, goldPaint)

        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(40, 184, 145, 59)
            style = Paint.Style.STROKE
            strokeWidth = 4f
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
            color = Color.parseColor("#F3DFA2")
            textSize = 30f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("KHABARNAMA • ${categoryLabel.uppercase()} DISPATCH", width / 2f, height / 2f + 50f, subPaint)

        return bitmap
    }
}

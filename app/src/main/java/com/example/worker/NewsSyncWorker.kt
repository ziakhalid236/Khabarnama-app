package com.example.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.DefaultNewsRepository
import com.example.data.SyncStatus
import com.example.data.local.AppDatabase
import com.example.data.remote.RssRemoteNewsSource
import com.example.model.AppLanguage
import com.example.model.NewsCategory
import com.example.util.NewsNotificationHelper
import java.util.concurrent.TimeUnit

class NewsSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val db = AppDatabase.getInstance(applicationContext)
        val remoteSource = RssRemoteNewsSource(applicationContext)
        val repository = DefaultNewsRepository(
            remoteSource = remoteSource,
            articleDao = db.articleDao(),
            context = applicationContext
        )

        val status = repository.syncFromRemote()
        if (status == SyncStatus.SUCCESS) {
            runCatching {
                val prefs = applicationContext.getSharedPreferences("khabarnama_prefs", Context.MODE_PRIVATE)
                val langCode = prefs.getString("selected_news_language", AppLanguage.ENGLISH.code) ?: AppLanguage.ENGLISH.code
                val currentLang = AppLanguage.fromCode(langCode)
                val alertPool = repository.getRecentAlertArticles(limit = 15)
                NewsNotificationHelper.notifyLatestBreakingNewsIfNew(
                    context = applicationContext,
                    articles = alertPool,
                    language = currentLang
                )
            }
        }

        return when (status) {
            SyncStatus.SUCCESS, SyncStatus.OFFLINE_WITH_CACHE -> Result.success()
            SyncStatus.ERROR_EMPTY_CACHE -> Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "khabarnama_periodic_rss_sync"

        fun schedulePeriodicSync(context: Context) {
            try {
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()

                val syncRequest = PeriodicWorkRequestBuilder<NewsSyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build()

                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    syncRequest
                )
            } catch (_: Exception) {
                // WorkManager may not be initialized in unit test environments
            }
        }
    }
}

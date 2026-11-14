package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

/**
 * Guarantees FirebaseApp is initialized in all process entrypoints (Application, MainActivity,
 * WorkManager, and lazy AI/RemoteConfig repositories), including release builds where resource
 * lookup or provider ordering may need explicit fallback initialization.
 */
object FirebaseInitHelper {

    private const val TAG = "FirebaseInitHelper"

    @Volatile
    private var storedAppContext: Context? = null

    fun registerContext(context: Context) {
        val appCtx = context.applicationContext ?: context
        if (storedAppContext == null) {
            storedAppContext = appCtx
        }
        ensureInitialized(appCtx)
    }

    fun ensureInitialized(context: Context? = storedAppContext): Boolean {
        val appCtx = context?.applicationContext ?: context ?: storedAppContext ?: return false
        if (storedAppContext == null) {
            storedAppContext = appCtx
        }

        if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
            return true
        }

        synchronized(this) {
            if (FirebaseApp.getApps(appCtx).isNotEmpty()) {
                return true
            }

            // 1. Standard google-services resource initialization
            val primaryInit = runCatching { FirebaseApp.initializeApp(appCtx) }.getOrNull()
            if (primaryInit != null || FirebaseApp.getApps(appCtx).isNotEmpty()) {
                return true
            }

            // 2. Fallback: explicitly build FirebaseOptions from string resources if provider/package lookup returned null
            val options = buildOptionsFromResources(appCtx)
            if (options != null) {
                val explicitInit = runCatching { FirebaseApp.initializeApp(appCtx, options) }.getOrNull()
                if (explicitInit != null || FirebaseApp.getApps(appCtx).isNotEmpty()) {
                    Log.d(TAG, "FirebaseApp initialized via explicit resource options fallback.")
                    return true
                }
            }

            Log.e(TAG, "Unable to initialize FirebaseApp: google_app_id / google_api_key resources missing.")
            return false
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun buildOptionsFromResources(context: Context): FirebaseOptions? {
        val googleAppId = resolveStringResource(context, "google_app_id")
        val apiKey = resolveStringResource(context, "google_api_key")
        val projectId = resolveStringResource(context, "project_id")
        if (googleAppId.isNullOrBlank() || apiKey.isNullOrBlank()) {
            return null
        }

        val builder = FirebaseOptions.Builder()
            .setApplicationId(googleAppId)
            .setApiKey(apiKey)

        if (!projectId.isNullOrBlank()) {
            builder.setProjectId(projectId)
        }
        resolveStringResource(context, "gcm_defaultSenderId")?.takeIf { it.isNotBlank() }?.let {
            builder.setGcmSenderId(it)
        }
        resolveStringResource(context, "google_storage_bucket")?.takeIf { it.isNotBlank() }?.let {
            builder.setStorageBucket(it)
        }
        resolveStringResource(context, "firebase_database_url")?.takeIf { it.isNotBlank() }?.let {
            builder.setDatabaseUrl(it)
        }

        return builder.build()
    }

    @SuppressLint("DiscouragedApi")
    private fun resolveStringResource(context: Context, resName: String): String? {
        val packagesToTry = listOf(context.packageName, "com.example").distinct()
        for (pkg in packagesToTry) {
            val id = context.resources.getIdentifier(resName, "string", pkg)
            if (id != 0) {
                val value = runCatching { context.getString(id) }.getOrNull()
                if (!value.isNullOrBlank()) return value
            }
        }
        return null
    }
}

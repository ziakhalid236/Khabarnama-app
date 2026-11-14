package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.recaptcha.RecaptchaAppCheckProviderFactory

private const val TAG = "AppCheckInit"

private const val DEBUG_TOKEN_EXTRA = "FIREBASE_APPCHECK_DEBUG_TOKEN"
private const val DEBUG_STORE_PREFIX = "com.google.firebase.appcheck.debug.store."
private const val DEBUG_SECRET_KEY = "com.google.firebase.appcheck.debug.DEBUG_SECRET"
private const val RECAPTCHA_SITE_KEY_RES = "recaptcha_site_key"

@SuppressLint("ApplySharedPref", "DiscouragedApi")
fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (!FirebaseInitHelper.ensureInitialized(context)) {
            Log.e(TAG, "FirebaseApp could not be initialized before App Check setup.")
            return
        }

        val siteKey = resolveRecaptchaSiteKey(context)

        if (BuildConfig.DEBUG) {
            val persistenceKey = FirebaseApp.getInstance().persistenceKey
            val prefs = context.getSharedPreferences(
                "$DEBUG_STORE_PREFIX$persistenceKey",
                Context.MODE_PRIVATE
            )
            val debugToken = intent?.getStringExtra(DEBUG_TOKEN_EXTRA)
            val existingSecret = prefs.getString(DEBUG_SECRET_KEY, null)

            if (!debugToken.isNullOrEmpty()) {
                // CRITICAL: MUST use commit() synchronously before DebugAppCheckProviderFactory reads it.
                prefs.edit()
                    .putString(DEBUG_SECRET_KEY, debugToken)
                    .commit()
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "App Check debug secret configured and DebugAppCheckProviderFactory installed.")
            } else if (!existingSecret.isNullOrBlank()) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.d(TAG, "Existing App Check debug secret found; DebugAppCheckProviderFactory installed.")
            } else if (!siteKey.isNullOrBlank()) {
                // Sideloaded/real-device debug launch without test-harness intent extra:
                // use the provisioned reCAPTCHA Enterprise key rather than an unregistered random UUID.
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                Log.d(TAG, "No debug token extra; RecaptchaAppCheckProviderFactory installed.")
            } else {
                Log.w(TAG, "No debug token or reCAPTCHA site key found; installing DebugAppCheckProviderFactory.")
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
            }
        } else {
            if (!siteKey.isNullOrBlank()) {
                FirebaseAppCheck.getInstance().installAppCheckProviderFactory(
                    RecaptchaAppCheckProviderFactory.getInstance(siteKey)
                )
                Log.d(TAG, "RecaptchaAppCheckProviderFactory installed.")
            } else {
                Log.e(TAG, "recaptcha_site_key string resource not found or blank in package.")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Firebase App Check", e)
    }
}

@SuppressLint("DiscouragedApi")
internal fun resolveRecaptchaSiteKey(context: Context): String? {
    val packagesToTry = listOf(context.packageName, "com.example").distinct()
    for (pkg in packagesToTry) {
        val resId = context.resources.getIdentifier(RECAPTCHA_SITE_KEY_RES, "string", pkg)
        if (resId != 0) {
            val value = runCatching { context.getString(resId).trim() }.getOrNull()
            if (!value.isNullOrBlank()) return value
        }
    }
    return null
}

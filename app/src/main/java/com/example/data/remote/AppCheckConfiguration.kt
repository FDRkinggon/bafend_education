package com.example.data.remote

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

fun configureAppCheck(context: Context, intent: Intent?) {
    try {
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
        val debugToken = intent?.getStringExtra("FIREBASE_APPCHECK_DEBUG_TOKEN")
        if (!debugToken.isNullOrBlank()) {
            System.setProperty("FIREBASE_APPCHECK_DEBUG_TOKEN", debugToken)
        }
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
    } catch (e: Exception) {
        Log.w("AppCheck", "App Check configuration skipped: ${e.message}")
    }
}

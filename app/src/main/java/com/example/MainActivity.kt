package com.example

import android.app.Application
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.authStateFlow
import com.example.data.remote.configureAppCheck
import com.example.ui.AuthScreen
import com.example.ui.ProfileScreen
import com.example.ui.ProfileViewModel
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val isRobolectric = Build.FINGERPRINT.equals("robolectric", ignoreCase = true)
        if (isRobolectric) {
            configureAppCheck(this, intent)
        }
        setContent {
            MyApplicationTheme {
                var isWindowSurfaceReady by remember { mutableStateOf(isRobolectric) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isWindowSurfaceReady) {
                        AuraProfileApp()
                    }
                }
                if (!isWindowSurfaceReady) {
                    LaunchedEffect(Unit) {
                        // Allow WindowManager's initial SurfaceSyncGroup transactions
                        // (wmsSync-VRI[MainActivity]#2 & VRI[MainActivity]#3) to commit cleanly first.
                        withFrameNanos { }
                        withFrameNanos { }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            configureAppCheck(applicationContext, intent)
                        }
                        kotlinx.coroutines.delay(120)
                        isWindowSurfaceReady = true
                    }
                }
            }
        }
    }
}

@Composable
fun AuraProfileApp(
    auth: FirebaseAuth = Firebase.auth
) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        // Unauthenticated state: Only display the sign-up / login screen.
        // Feature ViewModels and Firestore listeners are NOT created until authenticated.
        AuthScreen(
            onAuthSuccess = { /* AuthStateListener automatically transitions to ProfileScreen */ }
        )
    } else {
        val context = LocalContext.current
        val application = context.applicationContext as Application
        val profileViewModel: ProfileViewModel = viewModel(
            key = "profile_${user.uid}",
            factory = ProfileViewModel.Factory(
                application = application,
                currentUserId = user.uid,
                defaultDisplayName = user.displayName.orEmpty(),
                defaultEmail = user.email.orEmpty(),
                defaultPhotoUrl = user.photoUrl?.toString().orEmpty(),
                isEmailVerified = user.isEmailVerified
            )
        )
        ProfileScreen(
            viewModel = profileViewModel,
            isEmailVerified = user.isEmailVerified,
            onSignedOut = { /* AuthStateListener automatically transitions back to AuthScreen */ }
        )
    }
}

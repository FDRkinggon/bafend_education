package com.example.auth

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private val isInteractiveAuthInFlight = AtomicBoolean(false)

fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    trySend(currentUser)
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    if (currentUser != null) {
        scope.launch(Dispatchers.IO) {
            PersistentSessionManager(context).onUserAuthenticated(
                userId = currentUser.uid,
                displayName = currentUser.displayName.orEmpty(),
                email = currentUser.email.orEmpty(),
                photoUrl = currentUser.photoUrl?.toString().orEmpty()
            )
        }
        onAuthSuccess()
        return
    }

    val sessionManager = PersistentSessionManager(context)
    if (!sessionManager.isKeepSignedInEnabled() ||
        sessionManager.wasExplicitlySignedOut() ||
        !sessionManager.hasCreatedAccount()
    ) {
        onUnauthenticated()
        return
    }

    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onUnauthenticated()
        return
    }

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()

    // Strictly one option per GetCredentialRequest for silent auto sign-in
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    scope.launch {
        try {
            // Allow MainActivity's initial SurfaceSyncGroup (wmsSync-VRI[MainActivity]) to finish
            // before CredentialManager launches its Play Services HiddenActivity overlay.
            delay(1200L)
            if (isInteractiveAuthInFlight.get() || Firebase.auth.currentUser != null) {
                return@launch
            }
            val result = credentialManager.getCredential(context, request)
            if (isInteractiveAuthInFlight.get() || Firebase.auth.currentUser != null) {
                return@launch
            }
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                withContext(NonCancellable) {
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val signedInUser = authResult.user ?: Firebase.auth.currentUser
                    if (signedInUser != null) {
                        withContext(Dispatchers.IO) {
                            sessionManager.onUserAuthenticated(
                                userId = signedInUser.uid,
                                displayName = signedInUser.displayName.orEmpty(),
                                email = signedInUser.email.orEmpty(),
                                photoUrl = signedInUser.photoUrl?.toString().orEmpty()
                            )
                        }
                    }
                }
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (e: CancellationException) {
            // Normal composition transition or interactive sign-in preemption; do not treat as error
        } catch (e: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (e: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }

    // Strictly one option per GetCredentialRequest for interactive Google Sign-In
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()

    scope.launch {
        isInteractiveAuthInFlight.set(true)
        try {
            val activityContext = context as? Activity ?: context
            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                withContext(NonCancellable) {
                    val authResult = Firebase.auth.signInWithCredential(authCredential).await()
                    val signedInUser = authResult.user ?: Firebase.auth.currentUser
                    if (signedInUser != null) {
                        withContext(Dispatchers.IO) {
                            PersistentSessionManager(context).onUserAuthenticated(
                                userId = signedInUser.uid,
                                displayName = signedInUser.displayName.orEmpty(),
                                email = signedInUser.email.orEmpty(),
                                photoUrl = signedInUser.photoUrl?.toString().orEmpty()
                            )
                        }
                    }
                }
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type received from Google Sign-In.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: CancellationException) {
            // AuthScreen leaves the composition as soon as AuthStateListener emits the authenticated user.
            // Do not log LeftCompositionCancellationException as an Auth failure.
            if (Firebase.auth.currentUser != null) {
                onAuthSuccess()
            } else {
                onAuthCancelled()
            }
        } catch (e: Exception) {
            if (Firebase.auth.currentUser != null) {
                onAuthSuccess()
            } else {
                Log.e("Auth", "Google Sign-In failed", e)
                onAuthError(e.localizedMessage ?: "Sign in failed. Please try again.")
            }
        } finally {
            isInteractiveAuthInFlight.set(false)
        }
    }
}

fun signOutUser(
    context: Context,
    credentialManager: CredentialManager,
    onSignOutComplete: () -> Unit,
    scope: CoroutineScope
) {
    PersistentSessionManager(context).onExplicitSignOut()
    Firebase.auth.signOut()
    scope.launch {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.e("Auth", "Failed to clear credential state", e)
        } finally {
            onSignOutComplete()
        }
    }
}

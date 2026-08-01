package com.example.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import android.util.Log
import com.example.BuildConfig
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

data class GoogleUser(
    val uid: String,
    val email: String,
    val displayName: String?,
    val photoUrl: String?
)

sealed class AuthResult {
    data class Success(val user: GoogleUser) : AuthResult()
    data class Error(val message: String) : AuthResult()
    data object Cancelled : AuthResult()
}

/**
 * Google SSO via Credential Manager + Firebase Auth.
 * Requires `google-services.json` (or a valid web client ID) and Google Sign-In enabled in Firebase.
 */
class GoogleAuthRepository(private val context: Context) {

    private val auth: FirebaseAuth? by lazy {
        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseAuth.getInstance()
        }.getOrNull()
    }

    private val credentialManager = CredentialManager.create(context)

    val currentUser: FirebaseUser?
        get() = auth?.currentUser

    val isSignedIn: Boolean
        get() = currentUser != null

    fun currentGoogleUser(): GoogleUser? {
        val u = currentUser ?: return null
        return GoogleUser(
            uid = u.uid,
            email = u.email.orEmpty(),
            displayName = u.displayName,
            photoUrl = u.photoUrl?.toString()
        )
    }

    fun isFirebaseReady(): Boolean = auth != null

    private fun webClientId(): String {
        // 1) google-services plugin generated string
        val generated = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        if (generated != 0) {
            val value = context.getString(generated).trim()
            if (value.isNotBlank() && !value.contains("REPLACE")) return value
        }
        // 2) BuildConfig secret
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank() &&
            !BuildConfig.GOOGLE_WEB_CLIENT_ID.contains("REPLACE")
        ) {
            return BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
        }
        // 3) app string fallback
        return context.getString(R.string.google_web_client_id).trim()
    }

    /**
     * Launch Google Sign-In. Prefer one-tap authorized accounts, then full Sign-In With Google.
     */
    suspend fun signIn(): AuthResult {
        Log.d("GoogleAuth", "Starting sign-in process...")
        val firebaseAuth = auth
            ?: return AuthResult.Error(
                "Firebase is not initialized. Check your google-services.json."
            )
        val clientId = webClientId()
        Log.d("GoogleAuth", "Using Web Client ID: $clientId")

        if (clientId.isBlank() || clientId.contains("REPLACE")) {
            return AuthResult.Error(
                "Google Web Client ID is missing. Add it to .env or google-services.json."
            )
        }

        return try {
            val idToken = requestGoogleIdToken(clientId)
            if (idToken == null) {
                Log.e("GoogleAuth", "Failed to obtain ID Token from Credential Manager.")
                return AuthResult.Error("Could not get Google ID token.")
            }
            Log.d("GoogleAuth", "ID Token obtained. Signing in to Firebase...")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseAuth.signInWithCredential(credential).await()
            val user = result.user
                ?: return AuthResult.Error("Sign-in succeeded but no user returned.")

            Log.d("GoogleAuth", "Firebase sign-in success: ${user.email}")
            AuthResult.Success(
                GoogleUser(
                    uid = user.uid,
                    email = user.email.orEmpty(),
                    displayName = user.displayName,
                    photoUrl = user.photoUrl?.toString()
                )
            )
        } catch (e: GetCredentialCancellationException) {
            Log.d("GoogleAuth", "Sign-in cancelled by user.")
            AuthResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e("GoogleAuth", "No credentials found: ${e.message}")
            AuthResult.Error("No Google account found. Please add one in device settings.")
        } catch (e: Exception) {
            Log.e("GoogleAuth", "Sign-in exception: ${e.message}", e)
            AuthResult.Error(e.message ?: "Google sign-in failed.")
        }
    }

    private suspend fun requestGoogleIdToken(clientId: String): String? {
        // First try lightweight Google ID option (returning users)
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()
            val response = credentialManager.getCredential(context, request)
            return extractIdToken(response.credential)
        } catch (e: GetCredentialCancellationException) {
            throw e
        } catch (_: NoCredentialException) {
            // Fall through to full button flow
        } catch (_: Exception) {
            // Fall through
        }

        // Full "Sign in with Google" button experience
        val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        val response = credentialManager.getCredential(context, request)
        return extractIdToken(response.credential)
    }

    private fun extractIdToken(credential: androidx.credentials.Credential): String? {
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleId = GoogleIdTokenCredential.createFrom(credential.data)
            return googleId.idToken
        }
        return null
    }

    suspend fun signOut() {
        try {
            auth?.signOut()
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Best-effort local sign-out
            auth?.signOut()
        }
    }
}

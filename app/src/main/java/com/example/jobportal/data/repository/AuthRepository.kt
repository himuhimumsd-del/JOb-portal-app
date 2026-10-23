package com.example.jobportal.data.repository

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.jobportal.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
)

sealed interface AuthUiState {
    object Initializing : AuthUiState
    object Unauthenticated : AuthUiState
    object Authenticating : AuthUiState
    data class Authenticated(val user: AuthUser) : AuthUiState
    data class Error(val message: String) : AuthUiState
}

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "TalentNexusAuth"
    }

    private val _currentUser = MutableStateFlow<AuthUser?>(auth.currentUser?.toAuthUser())
    val currentUser: StateFlow<AuthUser?> = _currentUser.asStateFlow()

    private val _authUiState = MutableStateFlow<AuthUiState>(
        auth.currentUser?.let { AuthUiState.Authenticated(it.toAuthUser()) } ?: AuthUiState.Unauthenticated
    )
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser?.toAuthUser()
        _currentUser.value = user
        if (user != null) {
            _authUiState.value = AuthUiState.Authenticated(user)
        } else if (_authUiState.value !is AuthUiState.Authenticating) {
            _authUiState.value = AuthUiState.Unauthenticated
        }
    }

    init {
        auth.addAuthStateListener(authStateListener)
    }

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Interactive Google Sign-In using Jetpack Credential Manager.
     * Builds request with single GetSignInWithGoogleOption to ensure API 36 sheet responsiveness.
     */
    suspend fun signInWithGoogle(activity: Activity): Result<AuthUser> {
        _authUiState.value = AuthUiState.Authenticating

        val clientId = try {
            activity.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            val errorMsg = "Google Sign-In configuration missing: default_web_client_id not found"
            Log.e(TAG, errorMsg, e)
            _authUiState.value = AuthUiState.Error(errorMsg)
            return Result.failure(IllegalStateException(errorMsg, e))
        }

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val firebaseUser = authResult.user
                    ?: throw IllegalStateException("Firebase sign-in completed without user object")
                val authUser = firebaseUser.toAuthUser()
                _currentUser.value = authUser
                _authUiState.value = AuthUiState.Authenticated(authUser)
                Result.success(authUser)
            } else {
                val errorMsg = "Unexpected credential format returned by CredentialManager"
                Log.e(TAG, errorMsg)
                _authUiState.value = AuthUiState.Error(errorMsg)
                Result.failure(IllegalStateException(errorMsg))
            }
        } catch (e: GetCredentialCancellationException) {
            val isReauthFailure = e.message?.contains("Account reauth failed", ignoreCase = true) == true ||
                                  e.message?.contains("[16]", ignoreCase = true) == true
            if (isReauthFailure) {
                val errorMsg = "Google Account reauth failed on device/emulator. Please add a Google account in Android Settings -> Passwords & accounts, or test on a physical device."
                Log.w(TAG, errorMsg, e)
                _authUiState.value = AuthUiState.Error(errorMsg)
                Result.failure(IllegalStateException(errorMsg, e))
            } else {
                Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
                _authUiState.value = AuthUiState.Unauthenticated
                Result.failure(e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Sign-In failed", e)
            val errorMsg = e.localizedMessage ?: "Sign-in failed. Please try again."
            _authUiState.value = AuthUiState.Error(errorMsg)
            Result.failure(e)
        }
    }

    /**
     * Silent Auto-Sign-In for existing authorized sessions.
     * Uses GetGoogleIdOption with filterByAuthorizedAccounts = true.
     */
    suspend fun attemptAutoSignIn(context: Context): Boolean {
        if (auth.currentUser != null) {
            val user = auth.currentUser!!.toAuthUser()
            _currentUser.value = user
            _authUiState.value = AuthUiState.Authenticated(user)
            return true
        }

        val clientId = try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            return false
        }

        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(true)
            .setServerClientId(clientId)
            .setAutoSelectEnabled(true)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            val result = credentialManager.getCredential(context, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user?.toAuthUser()
                if (user != null) {
                    _currentUser.value = user
                    _authUiState.value = AuthUiState.Authenticated(user)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            // Auto sign-in is silent; non-fatal
            false
        }
    }

    /**
     * Secure Sign-Out clearing both Firebase session and CredentialManager credentials.
     */
    suspend fun signOut(context: Context) {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e(TAG, "Error during Firebase signOut", e)
        }

        try {
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (e: Exception) {
            Log.w(TAG, "Failed to clear CredentialManager state", e)
        } finally {
            _currentUser.value = null
            _authUiState.value = AuthUiState.Unauthenticated
        }
    }

    fun clearError() {
        if (_authUiState.value is AuthUiState.Error) {
            _authUiState.value = if (_currentUser.value != null) {
                AuthUiState.Authenticated(_currentUser.value!!)
            } else {
                AuthUiState.Unauthenticated
            }
        }
    }
}

private fun FirebaseUser.toAuthUser(): AuthUser {
    return AuthUser(
        uid = uid,
        email = email ?: "unknown@talentnexus.app",
        displayName = displayName ?: (email?.substringBefore('@') ?: "Executive User"),
        photoUrl = photoUrl?.toString()
    )
}

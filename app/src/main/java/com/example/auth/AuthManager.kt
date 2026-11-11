package com.example.auth

import android.content.Context
import androidx.credentials.CredentialManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Ponto de acesso e delegação para compatibilidade com o [FirebaseAuthManager].
 */
object AuthManager {

    val instance: FirebaseAuthManager
        get() = FirebaseAuthManager.getInstance()

    val auth: FirebaseAuth
        get() = instance.auth

    val currentUser: FirebaseUser?
        get() = instance.currentUser

    fun attemptAutoSignIn(
        context: Context,
        credentialManager: CredentialManager,
        onAuthSuccess: () -> Unit,
        onUnauthenticated: () -> Unit,
        scope: CoroutineScope
    ) {
        scope.launch {
            val ok = instance.attemptSilentSignIn(context)
            if (ok) onAuthSuccess() else onUnauthenticated()
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
        instance.signInWithGoogle(
            context = context,
            scope = scope,
            onAuthSuccess = { onAuthSuccess() },
            onAuthError = onAuthError,
            onCancelled = onAuthCancelled
        )
    }

    fun signOut(
        context: Context,
        credentialManager: CredentialManager,
        onSignOutComplete: () -> Unit,
        scope: CoroutineScope
    ) {
        scope.launch {
            instance.signOut(context)
            onSignOutComplete()
        }
    }
}

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
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Resultado padronizado das operações de autenticação do Firebase.
 */
sealed class FirebaseAuthResult {
    data class Success(val user: FirebaseUser, val isNewUser: Boolean = false) : FirebaseAuthResult()
    data object Cancelled : FirebaseAuthResult()
    data class Error(val message: String, val throwable: Throwable? = null) : FirebaseAuthResult()
}

/**
 * Gerenciador centralizado de autenticação do Firebase Auth SDK.
 * Integração com Jetpack Credential Manager (Google Sign-In), monitoramento de estado em tempo real
 * e envio de e-mails para recuperação/redefinição de acesso.
 */
class FirebaseAuthManager(
    val auth: FirebaseAuth = Firebase.auth
) {

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isUserLoggedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Flow reativo para observar o estado de autenticação em tempo real em Composables e ViewModels.
     */
    val authStateFlow: Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    /**
     * Tenta restaurar a sessão do usuário silenciosamente ao iniciar o app, sem abrir telas.
     */
    suspend fun attemptSilentSignIn(context: Context): Boolean {
        if (auth.currentUser != null) return true

        val clientId = resolveWebClientId(context) ?: return false
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
                auth.signInWithCredential(authCredential).await()
                auth.currentUser != null
            } else {
                false
            }
        } catch (e: Exception) {
            Log.d(TAG, "Login silencioso não encontrou credencial ativa salva: ${e.message}")
            false
        }
    }

    /**
     * Fluxo unificado de Login / Cadastro interativo com a conta Google do Associado.
     * No Firebase Auth, a credencial do Google cria automaticamente a conta no primeiro login
     * e autentica com segurança nos logins subsequentes.
     */
    suspend fun signInWithGoogle(activity: Activity): FirebaseAuthResult {
        val clientId = resolveWebClientId(activity)
            ?: return FirebaseAuthResult.Error("Configuração do Google Sign-In não encontrada (default_web_client_id).")

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        return try {
            val result = credentialManager.getCredential(activity, request)
            val credential = result.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                    ?: return FirebaseAuthResult.Error("Usuário nulo após autenticação com o Firebase.")

                val isNewUser = authResult.additionalUserInfo?.isNewUser == true
                FirebaseAuthResult.Success(user = user, isNewUser = isNewUser)
            } else {
                FirebaseAuthResult.Error("Tipo de credencial inválido recebido.")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-In cancelado ou dispensado pelo usuário: ${e.message}", e)
            FirebaseAuthResult.Cancelled
        } catch (e: Exception) {
            Log.e(TAG, "Falha na autenticação via Google Sign-In", e)
            FirebaseAuthResult.Error(e.localizedMessage ?: "Erro desconhecido ao entrar com Google.", e)
        }
    }

    /**
     * Versão baseada em callbacks para uso direto em Composables e botões da UI.
     */
    fun signInWithGoogle(
        context: Context,
        scope: CoroutineScope,
        onAuthSuccess: (FirebaseUser) -> Unit,
        onAuthError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) {
        val activity = context as? Activity
        if (activity == null) {
            onAuthError("O contexto deve ser uma Activity para exibir a tela de login.")
            return
        }

        scope.launch {
            when (val result = signInWithGoogle(activity)) {
                is FirebaseAuthResult.Success -> onAuthSuccess(result.user)
                is FirebaseAuthResult.Cancelled -> onCancelled()
                is FirebaseAuthResult.Error -> onAuthError(result.message)
            }
        }
    }

    /**
     * Alias semântico para Cadastro de Conta (utiliza o mesmo fluxo seguro de criação de usuário do Firebase).
     */
    fun signUpWithGoogle(
        context: Context,
        scope: CoroutineScope,
        onAuthSuccess: (FirebaseUser) -> Unit,
        onAuthError: (String) -> Unit,
        onCancelled: () -> Unit = {}
    ) = signInWithGoogle(context, scope, onAuthSuccess, onAuthError, onCancelled)

    /**
     * Recuperação de Senha / Acesso:
     * Envia um e-mail oficial do Firebase Auth com link seguro para redefinição de senha da conta do associado.
     */
    suspend fun sendPasswordResetEmail(email: String): Result<String> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Informe um endereço de e-mail válido para a recuperação."))
        }

        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Log.i(TAG, "E-mail de recuperação de acesso enviado para: $cleanEmail")
            Result.success("E-mail de recuperação enviado com sucesso para $cleanEmail. Verifique sua caixa de entrada.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException) {
            val userFriendlyMessage = when (e.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "Nenhuma conta associada encontrada para o e-mail informado."
                "ERROR_INVALID_EMAIL" -> "O formato do e-mail informado é inválido."
                "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas em sequência. Aguarde alguns instantes antes de tentar novamente."
                else -> e.localizedMessage ?: "Erro ao processar solicitação de recuperação de senha."
            }
            Log.e(TAG, "Erro do Firebase Auth ao enviar e-mail de recuperação: ${e.errorCode}", e)
            Result.failure(Exception(userFriendlyMessage, e))
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao enviar e-mail de recuperação de senha", e)
            Result.failure(e)
        }
    }

    /**
     * Atualização do Perfil cadastral do Firebase Auth (Nome de Exibição).
     */
    suspend fun updateProfile(displayName: String?): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Usuário não autenticado."))

        val profileUpdates = UserProfileChangeRequest.Builder()
            .apply {
                if (!displayName.isNullOrBlank()) setDisplayName(displayName)
            }
            .build()

        return try {
            user.updateProfile(profileUpdates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar perfil do Firebase Auth", e)
            Result.failure(e)
        }
    }

    /**
     * Encerra a sessão do associado no Firebase Auth e limpa as credenciais salvas no dispositivo.
     */
    suspend fun signOut(context: Context): Result<Unit> {
        return try {
            auth.signOut()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao encerrar sessão", e)
            Result.failure(e)
        }
    }

    private fun resolveWebClientId(context: Context): String? {
        return try {
            context.getString(R.string.default_web_client_id)
        } catch (e: Exception) {
            Log.e(TAG, "default_web_client_id não encontrado em resources: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "FirebaseAuthManager"

        @Volatile
        private var INSTANCE: FirebaseAuthManager? = null

        fun getInstance(): FirebaseAuthManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FirebaseAuthManager().also { INSTANCE = it }
            }
        }
    }
}

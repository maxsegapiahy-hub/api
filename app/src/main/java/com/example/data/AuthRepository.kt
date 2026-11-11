package com.example.data

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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Interface de contrato do repositório de autenticação para injeção de dependência em ViewModels e testes unitários.
 */
interface IAuthRepository {
    val currentUser: FirebaseUser?
    val isUserAuthenticated: Boolean
    val currentUserId: String?

    fun observeAuthState(): Flow<FirebaseUser?>
    suspend fun signIn(activity: Activity): Result<FirebaseUser>
    suspend fun signUp(activity: Activity): Result<FirebaseUser>
    suspend fun resetPassword(email: String): Result<String>
    suspend fun sendPasswordResetEmail(email: String): Result<String>
    suspend fun attemptSilentSignIn(context: Context): Result<FirebaseUser>
    suspend fun updateProfile(displayName: String): Result<Unit>
    suspend fun signOut(context: Context): Result<Unit>
}

/**
 * Repositório que encapsula as operações do Firebase Authentication,
 * utilizando o tipo padrão Kotlin `Result<T>` para retorno de sucesso ou falha,
 * pronto para ser injetado em ViewModels via injeção por construtor.
 */
class AuthRepository(
    private val auth: FirebaseAuth = Firebase.auth
) : IAuthRepository {

    override val currentUser: FirebaseUser?
        get() = auth.currentUser

    override val isUserAuthenticated: Boolean
        get() = auth.currentUser != null

    override val currentUserId: String?
        get() = auth.currentUser?.uid

    /**
     * Observa as alterações no estado de login do Firebase Auth em tempo real via Kotlin Flow.
     */
    override fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    /**
     * Autenticação / Login assíncrono de usuário retornando Kotlin `Result<FirebaseUser>`.
     */
    override suspend fun signIn(activity: Activity): Result<FirebaseUser> {
        return executeGoogleAuth(activity)
    }

    /**
     * Cadastro / Criação de conta assíncrona de novo associado retornando Kotlin `Result<FirebaseUser>`.
     */
    override suspend fun signUp(activity: Activity): Result<FirebaseUser> {
        return executeGoogleAuth(activity)
    }

    /**
     * Recuperação de Senha / Redefinição de Acesso por E-mail via Firebase Auth SDK.
     * Retorna `Result.success(mensagem)` ou `Result.failure(exception)`.
     */
    override suspend fun resetPassword(email: String): Result<String> {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            return Result.failure(IllegalArgumentException("Informe um endereço de e-mail válido para recuperação de senha."))
        }

        return try {
            auth.sendPasswordResetEmail(cleanEmail).await()
            Log.i(TAG, "E-mail de recuperação de senha enviado com sucesso para: $cleanEmail")
            Result.success("E-mail de recuperação de senha enviado com sucesso para $cleanEmail. Verifique sua caixa de entrada.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseAuthException) {
            val friendlyMsg = when (e.errorCode) {
                "ERROR_USER_NOT_FOUND" -> "Nenhum associado cadastrado com o e-mail informado."
                "ERROR_INVALID_EMAIL" -> "O formato do e-mail é inválido."
                "ERROR_TOO_MANY_REQUESTS" -> "Muitas tentativas recentes. Aguarde alguns minutos antes de tentar novamente."
                else -> e.localizedMessage ?: "Erro ao solicitar recuperação de senha no Firebase Auth."
            }
            Log.e(TAG, "Erro do Firebase Auth ao enviar e-mail de recuperação: ${e.errorCode}", e)
            Result.failure(Exception(friendlyMsg, e))
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao enviar e-mail de recuperação de senha", e)
            Result.failure(e)
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): Result<String> = resetPassword(email)

    /**
     * Restaura a sessão do associado de forma silenciosa e transparente na inicialização do app.
     */
    override suspend fun attemptSilentSignIn(context: Context): Result<FirebaseUser> {
        val user = auth.currentUser
        if (user != null) {
            return Result.success(user)
        }

        val clientId = resolveWebClientId(context)
            ?: return Result.failure(IllegalStateException("Configuração do Google Sign-In não encontrada (default_web_client_id)."))

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
            val credentialResult = credentialManager.getCredential(context, request)
            val credential = credentialResult.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val loggedUser = authResult.user
                if (loggedUser != null) {
                    Result.success(loggedUser)
                } else {
                    Result.failure(IllegalStateException("Usuário nulo após restaurar sessão."))
                }
            } else {
                Result.failure(IllegalStateException("Nenhuma credencial Google ativa salva no dispositivo."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Atualiza o nome cadastral do usuário no Firebase Auth.
     */
    override suspend fun updateProfile(displayName: String): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Usuário não está autenticado no momento."))

        val updates = UserProfileChangeRequest.Builder()
            .setDisplayName(displayName.trim())
            .build()

        return try {
            user.updateProfile(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar perfil no Firebase Auth", e)
            Result.failure(e)
        }
    }

    /**
     * Encerra a sessão do usuário no Firebase Auth e limpa as credenciais locais.
     */
    override suspend fun signOut(context: Context): Result<Unit> {
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

    private suspend fun executeGoogleAuth(activity: Activity): Result<FirebaseUser> {
        val clientId = resolveWebClientId(activity)
            ?: return Result.failure(IllegalStateException("Configuração do Google Sign-In não encontrada (default_web_client_id)."))

        val credentialManager = CredentialManager.create(activity)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

        return try {
            val credentialResult = credentialManager.getCredential(activity, request)
            val credential = credentialResult.credential
            if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
                val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                    ?: return Result.failure(IllegalStateException("Usuário não retornado após autenticação com Firebase."))

                Result.success(user)
            } else {
                Result.failure(IllegalArgumentException("Tipo de credencial incompatível recebido."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Autenticação cancelada pelo usuário: ${e.message}", e)
            Result.failure(CancellationException("Autenticação cancelada pelo usuário."))
        } catch (e: Exception) {
            Log.e(TAG, "Erro na autenticação com Firebase Auth", e)
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
        private const val TAG = "AuthRepository"

        @Volatile
        private var INSTANCE: AuthRepository? = null

        fun getInstance(): AuthRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthRepository().also { INSTANCE = it }
            }
        }
    }
}

package com.example.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AuthRepository
import com.example.data.IAuthRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: FirebaseUser? = null,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

/**
 * ViewModel que recebe o [AuthRepository] por injeção de construtor
 * e gerencia os estados de login, cadastro e recuperação de senha.
 */
class AuthViewModel(
    private val authRepository: IAuthRepository = AuthRepository.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(user = authRepository.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val currentAuthState: StateFlow<FirebaseUser?> = authRepository.observeAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = authRepository.currentUser
        )

    fun signIn(activity: Activity) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            authRepository.signIn(activity)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        successMessage = "Autenticado com sucesso na Conta Max!"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Falha ao realizar login."
                    )
                }
        }
    }

    fun signUp(activity: Activity) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            authRepository.signUp(activity)
                .onSuccess { user ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        user = user,
                        successMessage = "Conta criada com sucesso!"
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Falha ao criar conta."
                    )
                }
        }
    }

    fun resetPassword(email: String) {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, successMessage = null)
        viewModelScope.launch {
            authRepository.resetPassword(email)
                .onSuccess { message ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        successMessage = message
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Falha ao enviar e-mail de recuperação."
                    )
                }
        }
    }

    fun signOut(context: Context) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            authRepository.signOut(context)
                .onSuccess {
                    _uiState.value = AuthUiState(user = null, successMessage = "Sessão encerrada.")
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Erro ao encerrar sessão."
                    )
                }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }
}

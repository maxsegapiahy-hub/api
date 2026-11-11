package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AuthRepository
import com.example.data.IAuthRepository
import com.example.ui.components.SpartanBadgeVector
import com.example.ui.theme.*
import com.example.ui.viewmodel.AuthViewModel
import com.example.util.LocationHelper
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

enum class AuthMode(val title: String) {
    SIGN_IN("Entrar"),
    SIGN_UP("Criar Conta")
}

/**
 * Tela de autenticação completa em Jetpack Compose integrando o [AuthRepository].
 * Inclui campos de texto para e-mail e senha, alternância entre Sign In e Sign Up,
 * recuperação de senha e botão com Google Sign-In oficial provisionado pelo Firebase.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    authRepository: IAuthRepository = remember { AuthRepository.getInstance() },
    viewModel: AuthViewModel = viewModel(),
    onAuthSuccess: (FirebaseUser) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val scrollState = rememberScrollState()

    var authMode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaxDarkBackground,
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (authMode == AuthMode.SIGN_IN) "Acesso do Associado" else "Novo Cadastro Max",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.testTag("auth_btn_back")
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaxDarkSurface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // BRANDING HERO
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(MaxBurgundyDark, CircleShape)
                    .border(2.dp, MaxGold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                SpartanBadgeVector(
                    size = 38.dp,
                    metalColor = MaxGold,
                    baseColor = MaxBurgundyDark
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "MAX SEG APIAHY",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Salvaguarda, Tecnologia & Clube de Benefícios",
                    color = MaxGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // TAB SELECTOR: SIGN IN vs SIGN UP
            Surface(
                color = MaxDarkSurface,
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxBorderGray, MaxBurgundyDark))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    AuthMode.entries.forEach { mode ->
                        val isSelected = authMode == mode
                        Button(
                            onClick = {
                                authMode = mode
                                errorMessage = null
                                successMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaxBurgundyDark else Color.Transparent,
                                contentColor = if (isSelected) Color.White else MaxTextMuted
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("tab_${mode.name.lowercase()}")
                        ) {
                            Text(
                                text = mode.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // FEEDBACK BANNER (ERROR / SUCCESS)
            AnimatedVisibility(visible = errorMessage != null) {
                Surface(
                    color = Color(0xFF4A1010),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF8A80))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            AnimatedVisibility(visible = successMessage != null) {
                Surface(
                    color = Color(0xFF103A16),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF69F0AE))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successMessage ?: "",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // FORM FIELDS CARD
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaxDarkSurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxBorderGray, MaxBurgundyLight)))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // EMAIL TEXT FIELD
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = null
                        },
                        label = { Text("E-mail do Associado", color = MaxTextMuted) },
                        placeholder = { Text("seuemail@maxseg.com.br", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Email, contentDescription = null, tint = MaxGold)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold,
                            unfocusedBorderColor = MaxBorderGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input")
                    )

                    // PASSWORD TEXT FIELD
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = { Text("Senha", color = MaxTextMuted) },
                        placeholder = { Text("••••••••", color = Color.Gray) },
                        leadingIcon = {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaxGold)
                        },
                        trailingIcon = {
                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isPasswordVisible) "Ocultar senha" else "Mostrar senha",
                                    tint = MaxTextMuted
                                )
                            }
                        },
                        visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = if (authMode == AuthMode.SIGN_UP) ImeAction.Next else ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) },
                            onDone = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold,
                            unfocusedBorderColor = MaxBorderGray
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input")
                    )

                    // CONFIRM PASSWORD FIELD (SIGN UP ONLY)
                    if (authMode == AuthMode.SIGN_UP) {
                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMessage = null
                            },
                            label = { Text("Confirmar Senha", color = MaxTextMuted) },
                            placeholder = { Text("••••••••", color = Color.Gray) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaxGold)
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { focusManager.clearFocus() }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = MaxGold,
                                unfocusedBorderColor = MaxBorderGray
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_password_input")
                        )
                    }

                    // FORGOT PASSWORD LINK (SIGN IN ONLY)
                    if (authMode == AuthMode.SIGN_IN) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier.testTag("forgot_password_button")
                            ) {
                                Text(
                                    text = "Esqueceu a senha?",
                                    color = MaxGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // PRIMARY ACTION BUTTON (SIGN IN / SIGN UP)
                    Button(
                        onClick = {
                            if (activity == null) {
                                errorMessage = "Activity indisponível para autenticação."
                                return@Button
                            }

                            if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                                errorMessage = "Por favor, digite um formato de e-mail válido."
                                return@Button
                            }

                            if (authMode == AuthMode.SIGN_UP && password.isNotBlank() && confirmPassword.isNotBlank() && password != confirmPassword) {
                                errorMessage = "As senhas digitadas não coincidem."
                                return@Button
                            }

                            isLoading = true
                            errorMessage = null
                            successMessage = null

                            scope.launch {
                                val result = if (authMode == AuthMode.SIGN_IN) {
                                    authRepository.signIn(activity)
                                } else {
                                    authRepository.signUp(activity)
                                }

                                isLoading = false
                                result.onSuccess { user ->
                                    successMessage = if (authMode == AuthMode.SIGN_IN)
                                        "Login realizado com sucesso!"
                                    else
                                        "Conta criada e vinculada com sucesso!"
                                    onAuthSuccess(user)
                                }.onFailure { error ->
                                    errorMessage = error.localizedMessage ?: "Falha na autenticação."
                                }
                            }
                        },
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundyDark),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag(if (authMode == AuthMode.SIGN_IN) "sign_in_button" else "sign_up_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White)
                        } else {
                            Icon(
                                imageVector = if (authMode == AuthMode.SIGN_IN) Icons.Default.Login else Icons.Default.PersonAdd,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (authMode == AuthMode.SIGN_IN) "Entrar na Conta" else "Criar Minha Conta",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // OR DIVIDER
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Divider(modifier = Modifier.weight(1f), color = MaxBorderGray)
                Text(
                    text = "  OU ACESSE COM  ",
                    color = MaxTextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Divider(modifier = Modifier.weight(1f), color = MaxBorderGray)
            }

            // OFFICIAL GOOGLE SIGN-IN BUTTON (REQUIRED CREDENTIAL MANAGER INTEGRATION)
            Button(
                onClick = {
                    if (activity == null) {
                        errorMessage = "Activity indisponível para autenticação."
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    successMessage = null

                    scope.launch {
                        authRepository.signIn(activity)
                            .onSuccess { user ->
                                isLoading = false
                                successMessage = "Autenticado com sucesso via Google!"
                                onAuthSuccess(user)
                            }
                            .onFailure { error ->
                                isLoading = false
                                errorMessage = error.localizedMessage ?: "Falha ao autenticar com o Google."
                            }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("google_signin_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = Color(0xFF4285F4),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (authMode == AuthMode.SIGN_IN) "Entrar com Google" else "Cadastrar com Google",
                        color = Color(0xFF1F1F1F),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // SECURITY NOTICE
            Surface(
                color = MaxDarkSurface,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaxGold, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Seus dados estão protegidos pela criptografia de nível bancário da Central Max Seg e Google Firebase Security.",
                        color = MaxTextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }

    // PASSWORD RESET DIALOG INTEGRATING resetPassword(email)
    if (showResetDialog) {
        var resetEmailInput by remember { mutableStateOf(email) }
        var isResetting by remember { mutableStateOf(false) }
        var resetResultMsg by remember { mutableStateOf<String?>(null) }
        var resetHasError by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                if (!isResetting) showResetDialog = false
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LockReset, contentDescription = null, tint = MaxGold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Recuperar Senha", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Digite seu e-mail cadastrado para receber o link oficial de redefinição de acesso enviado pelo Firebase:",
                        color = MaxTextMuted,
                        fontSize = 12.sp
                    )

                    OutlinedTextField(
                        value = resetEmailInput,
                        onValueChange = {
                            resetEmailInput = it
                            resetResultMsg = null
                        },
                        label = { Text("E-mail Cadastrado", color = MaxTextMuted) },
                        placeholder = { Text("exemplo@maxseg.com.br", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reset_email_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold,
                            unfocusedBorderColor = MaxBorderGray
                        )
                    )

                    if (resetResultMsg != null) {
                        Surface(
                            color = if (resetHasError) Color(0xFF4A1010) else Color(0xFF103A16),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = resetResultMsg ?: "",
                                color = if (resetHasError) Color(0xFFFF8A80) else Color(0xFF69F0AE),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (resetEmailInput.isNotBlank()) {
                                isResetting = true
                                resetResultMsg = null
                                scope.launch {
                                    val result = authRepository.resetPassword(resetEmailInput)
                                    isResetting = false
                                    result.onSuccess { msg ->
                                        resetHasError = false
                                        resetResultMsg = msg
                                    }.onFailure { err ->
                                        resetHasError = true
                                        resetResultMsg = err.localizedMessage ?: "Falha ao enviar e-mail."
                                    }
                                }
                            } else {
                                resetHasError = true
                                resetResultMsg = "Por favor, digite seu endereço de e-mail."
                            }
                        },
                        enabled = !isResetting,
                        colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundyDark),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_send_reset_password")
                    ) {
                        if (isResetting) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White)
                        } else {
                            Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Enviar Link de Redefinição", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Divider(color = MaxBorderGray)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Plantão Max Seg Apiaí 24h:", color = MaxTextMuted, fontSize = 10.sp)
                            Text("(15) 99700-0000", color = MaxGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                LocationHelper.openDialer(context, "(15) 99700-0000")
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = MaxGold, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ligar", color = MaxGold, fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Fechar", color = Color.White)
                }
            },
            containerColor = MaxDarkSurface
        )
    }
}

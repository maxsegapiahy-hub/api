package com.example.ui.screens

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.example.auth.AuthManager
import com.example.auth.FirebaseAuthManager
import com.example.data.MaxSegCloudRepository
import com.example.model.EmergencyContact
import com.example.model.MemberCardData
import com.example.model.SosAlert
import com.example.model.UserProfile
import com.example.ui.components.SpartanBadgeVector
import com.example.ui.theme.*
import com.example.util.LocationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

enum class AccountTab(val title: String) {
    PROFILE("Conta & Perfil"),
    CONTACTS("Contatos SOS"),
    DISPATCH("Central 24h & GPS")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    val cloudRepo = remember { MaxSegCloudRepository(context) }

    var currentUser by remember { mutableStateOf(AuthManager.currentUser) }
    var selectedTab by remember { mutableStateOf(AccountTab.PROFILE) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    // Dialogs
    var showAddContactDialog by remember { mutableStateOf(false) }

    // Local profile data for sync
    val localMemberData = remember { MemberCardData() }
    var cloudProfile by remember { mutableStateOf<UserProfile?>(null) }
    var emergencyContacts by remember { mutableStateOf<List<EmergencyContact>>(emptyList()) }
    var sosAlerts by remember { mutableStateOf<List<SosAlert>>(emptyList()) }

    // Form fields for editing/registration
    var fullNameInput by remember { mutableStateOf(localMemberData.memberName) }
    var emailInput by remember { mutableStateOf(currentUser?.email ?: "associado@maxseg.com.br") }
    var documentCpfInput by remember { mutableStateOf("123.456.789-00") }
    var phoneInput by remember { mutableStateOf(localMemberData.emergencyPhone) }
    var bloodTypeInput by remember { mutableStateOf(localMemberData.bloodType) }
    var cityInput by remember { mutableStateOf(localMemberData.city) }

    // Auth state listener
    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
            if (auth.currentUser != null) {
                emailInput = auth.currentUser?.email ?: emailInput
            }
        }
        AuthManager.auth.addAuthStateListener(listener)
        onDispose {
            AuthManager.auth.removeAuthStateListener(listener)
        }
    }

    // Observe Firestore profile & contacts when authenticated
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            launch {
                cloudRepo.observeUserProfile(uid).collect { profile ->
                    cloudProfile = profile
                    if (profile != null) {
                        fullNameInput = profile.fullName
                        phoneInput = profile.phone
                        documentCpfInput = profile.documentCpf
                        bloodTypeInput = profile.bloodType
                        cityInput = profile.city
                    }
                }
            }
            launch {
                cloudRepo.observeEmergencyContacts(uid).collect { list ->
                    emergencyContacts = list
                }
            }
            launch {
                cloudRepo.observeSosAlerts(uid).collect { alerts ->
                    sosAlerts = alerts
                }
            }
        } else {
            cloudProfile = null
            emergencyContacts = emptyList()
            sosAlerts = emptyList()
        }
    }

    if (currentUser == null) {
        AuthScreen(
            modifier = modifier,
            onAuthSuccess = { user ->
                currentUser = user
                successMessage = "Autenticado com sucesso na Conta Max!"
            }
        )
    } else {
        Scaffold(
            containerColor = MaxDarkBackground,
            modifier = modifier.fillMaxSize()
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // TOP STATUS HERO
            item {
                AccountHeaderCard(
                    currentUser = currentUser,
                    cloudProfile = cloudProfile,
                    onSignOut = {
                        isLoading = true
                        AuthManager.signOut(context, credentialManager, onSignOutComplete = {
                            isLoading = false
                            currentUser = null
                            Toast.makeText(context, "Sessão encerrada", Toast.LENGTH_SHORT).show()
                        }, scope)
                    }
                )
            }

            // FEEDBACK BANNERS
            if (errorMessage != null) {
                item {
                    Surface(
                        color = Color(0xFF5A1010),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF8A80))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(errorMessage ?: "", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            if (successMessage != null) {
                item {
                    Surface(
                        color = Color(0xFF1B5E20),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF69F0AE))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(successMessage ?: "", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
            }

            // TAB NAVIGATION
            item {
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = MaxDarkSurface,
                    contentColor = MaxGold,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = MaxGold,
                            height = 3.dp
                        )
                    }
                ) {
                    AccountTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (selectedTab == tab) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }

            // TAB CONTENT
            when (selectedTab) {
                AccountTab.PROFILE -> {
                    item {
                        AuthenticatedProfileSection(
                            fullName = fullNameInput,
                            onFullNameChange = { fullNameInput = it },
                            email = currentUser?.email ?: emailInput,
                            documentCpf = documentCpfInput,
                            onDocumentCpfChange = { documentCpfInput = it },
                            phone = phoneInput,
                            onPhoneChange = { phoneInput = it },
                            bloodType = bloodTypeInput,
                            onBloodTypeChange = { bloodTypeInput = it },
                            city = cityInput,
                            onCityChange = { cityInput = it },
                            isLoading = isLoading,
                            onSave = {
                                isLoading = true
                                errorMessage = null
                                successMessage = null
                                scope.launch {
                                    val updatedProfile = UserProfile(
                                        userId = currentUser!!.uid,
                                        fullName = fullNameInput,
                                        email = currentUser!!.email ?: emailInput,
                                        documentCpf = documentCpfInput,
                                        phone = phoneInput,
                                        bloodType = bloodTypeInput,
                                        city = cityInput,
                                        tier = cloudProfile?.tier ?: localMemberData.tier.name,
                                        cardCode = cloudProfile?.cardCode ?: localMemberData.memberId,
                                        status = "ATIVO"
                                    )
                                    val result = cloudRepo.saveUserProfile(updatedProfile)
                                    isLoading = false
                                    if (result.isSuccess) {
                                        successMessage = "Dados atualizados e sincronizados na nuvem Max Seg!"
                                    } else {
                                        errorMessage = "Erro ao salvar perfil: ${result.exceptionOrNull()?.message}"
                                    }
                                }
                            }
                        )
                    }
                }

                AccountTab.CONTACTS -> {
                    item {
                        EmergencyContactsHeader(
                            onAddClick = {
                                if (currentUser == null) {
                                    Toast.makeText(context, "Acesse sua Conta Max para gerenciar contatos na nuvem", Toast.LENGTH_SHORT).show()
                                } else {
                                    showAddContactDialog = true
                                }
                            }
                        )
                    }

                    if (emergencyContacts.isEmpty()) {
                        item {
                            EmptyContactsPlaceholder(
                                isAuthenticated = currentUser != null,
                                onAddClick = { showAddContactDialog = true }
                            )
                        }
                    } else {
                        items(emergencyContacts) { contact ->
                            EmergencyContactCard(
                                contact = contact,
                                onCall = { LocationHelper.openDialer(context, contact.phone) },
                                onWhatsApp = {
                                    val coords = LocationHelper.getCurrentCoordinates(context)
                                    LocationHelper.openWhatsAppEmergency(
                                        context,
                                        contact.phone,
                                        fullNameInput,
                                        coords.latitude,
                                        coords.longitude
                                    )
                                },
                                onDelete = {
                                    scope.launch {
                                        cloudRepo.deleteEmergencyContact(contact.id)
                                        Toast.makeText(context, "Contato removido", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                }

                AccountTab.DISPATCH -> {
                    item {
                        CentralMaxDispatchSection(
                            userName = fullNameInput,
                            isAuthenticated = currentUser != null,
                            cloudRepo = cloudRepo,
                            sosAlerts = sosAlerts,
                            onAlertDispatched = { alert ->
                                successMessage = "SOS enviado para a Central 24h! Viatura ${alert.dispatchedUnit} em deslocamento."
                            }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }

    // MODAL ADICIONAR CONTATO DE EMERGÊNCIA
    if (showAddContactDialog) {
        var contactName by remember { mutableStateOf("") }
        var contactRel by remember { mutableStateOf("") }
        var contactPhone by remember { mutableStateOf("") }
        var isPrimaryContact by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = {
                Text("Novo Contato de Emergência", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Nome do Contato", color = MaxTextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("input_contact_name"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold,
                            unfocusedBorderColor = MaxBorderGray
                        )
                    )
                    OutlinedTextField(
                        value = contactRel,
                        onValueChange = { contactRel = it },
                        label = { Text("Parentesco (Ex: Cônjuge, Filho, Irmão)", color = MaxTextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("input_contact_rel"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold,
                            unfocusedBorderColor = MaxBorderGray
                        )
                    )
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("Telefone / WhatsApp com DDD", color = MaxTextMuted) },
                        modifier = Modifier.fillMaxWidth().testTag("input_contact_phone"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold,
                            unfocusedBorderColor = MaxBorderGray
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Checkbox(
                            checked = isPrimaryContact,
                            onCheckedChange = { isPrimaryContact = it },
                            colors = CheckboxDefaults.colors(checkedColor = MaxGold)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Contato preferencial para socorro", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (contactName.isNotBlank() && contactPhone.isNotBlank()) {
                            scope.launch {
                                val newContact = EmergencyContact(
                                    userId = currentUser?.uid ?: "",
                                    name = contactName,
                                    relationship = contactRel.ifBlank { "Contato de Emergência" },
                                    phone = contactPhone,
                                    isPrimary = isPrimaryContact
                                )
                                cloudRepo.addEmergencyContact(newContact)
                                showAddContactDialog = false
                                Toast.makeText(context, "Contato de emergência salvo!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Preencha nome e telefone", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundyDark),
                    modifier = Modifier.testTag("btn_save_contact")
                ) {
                    Text("Salvar Contato", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancelar", color = MaxTextMuted)
                }
            },
            containerColor = MaxDarkSurface
        )
    }
    }
}

@Composable
fun AccountHeaderCard(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    cloudProfile: UserProfile?,
    onSignOut: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaxDarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.horizontalGradient(
                listOf(MaxBurgundyDark, MaxGold.copy(alpha = 0.5f))
            )
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaxBurgundyDark, CircleShape)
                            .border(1.5.dp, MaxGold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        SpartanBadgeVector(
                            size = 28.dp,
                            metalColor = MaxGold,
                            baseColor = MaxBurgundyDark
                        )
                    }

                    Column {
                        Text(
                            text = if (currentUser != null) (cloudProfile?.fullName ?: currentUser.displayName ?: "Associado Max") else "Visitante / Convidado",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (currentUser != null) MaxGreenSuccess else Color.Gray,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (currentUser != null) "Sincronizado na Nuvem Max" else "Modo Offline / Não autenticado",
                                color = if (currentUser != null) MaxGreenSuccess else MaxTextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                if (currentUser != null) {
                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier.testTag("btn_logout")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Sair da Conta",
                            tint = Color(0xFFFF8A80)
                        )
                    }
                }
            }

            if (currentUser != null) {
                Surface(
                    color = MaxDarkBackground,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Plano: ${cloudProfile?.tier ?: "TOTAL VIP"}",
                            color = MaxGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Carteirinha: ${cloudProfile?.cardCode ?: "00.584.2024-A"}",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}



@Composable
fun AuthenticatedProfileSection(
    fullName: String,
    onFullNameChange: (String) -> Unit,
    email: String,
    documentCpf: String,
    onDocumentCpfChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    bloodType: String,
    onBloodTypeChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    isLoading: Boolean,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaxDarkSurface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Dados Cadastrais do Titular",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = onFullNameChange,
                label = { Text("Nome Completo", color = MaxTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaxGold,
                    unfocusedBorderColor = MaxBorderGray
                )
            )

            OutlinedTextField(
                value = email,
                onValueChange = {},
                readOnly = true,
                label = { Text("E-mail Vinculado (Google)", color = MaxTextMuted) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White.copy(alpha = 0.7f),
                    unfocusedTextColor = Color.White.copy(alpha = 0.7f),
                    focusedBorderColor = MaxBorderGray,
                    unfocusedBorderColor = MaxBorderGray
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = documentCpf,
                    onValueChange = onDocumentCpfChange,
                    label = { Text("CPF / Doc", color = MaxTextMuted) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaxGold,
                        unfocusedBorderColor = MaxBorderGray
                    )
                )

                OutlinedTextField(
                    value = bloodType,
                    onValueChange = onBloodTypeChange,
                    label = { Text("Tipo Sang.", color = MaxTextMuted) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaxGold,
                        unfocusedBorderColor = MaxBorderGray
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = phone,
                    onValueChange = onPhoneChange,
                    label = { Text("Celular / WhatsApp", color = MaxTextMuted) },
                    modifier = Modifier.weight(1.2f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaxGold,
                        unfocusedBorderColor = MaxBorderGray
                    )
                )

                OutlinedTextField(
                    value = city,
                    onValueChange = onCityChange,
                    label = { Text("Cidade", color = MaxTextMuted) },
                    modifier = Modifier.weight(0.8f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaxGold,
                        unfocusedBorderColor = MaxBorderGray
                    )
                )
            }

            Button(
                onClick = onSave,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundyDark),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_profile")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Salvar e Sincronizar na Nuvem", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmergencyContactsHeader(
    onAddClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                "Contatos de Emergência SOS",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Notificados imediatamente quando você acionar o socorro",
                color = MaxTextMuted,
                fontSize = 11.sp
            )
        }

        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundyDark),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.testTag("btn_open_add_contact")
        ) {
            Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Adicionar", color = Color.White, fontSize = 12.sp)
        }
    }
}

@Composable
fun EmptyContactsPlaceholder(
    isAuthenticated: Boolean,
    onAddClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaxDarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.ContactPhone,
                contentDescription = null,
                tint = MaxGold,
                modifier = Modifier.size(40.dp)
            )
            Text(
                "Nenhum contato de emergência cadastrado",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isAuthenticated)
                    "Adicione familiares, cônjuge ou amigos para receberem seus alertas SOS com mapa GPS em tempo real."
                else
                    "Faça login na Conta Max para salvar e sincronizar seus contatos na nuvem.",
                color = MaxTextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            if (isAuthenticated) {
                OutlinedButton(
                    onClick = onAddClick,
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(MaxGold, MaxBurgundyLight)))
                ) {
                    Text("+ Adicionar Primeiro Contato", color = MaxGold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun EmergencyContactCard(
    contact: EmergencyContact,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaxDarkSurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxBorderGray, MaxBurgundyDark)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaxBurgundyDark, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = MaxGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(contact.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        if (contact.isPrimary) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = MaxGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    "PRIORITÁRIO",
                                    color = MaxGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        "${contact.relationship} • ${contact.phone}",
                        color = MaxTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCall) {
                    Icon(Icons.Default.Phone, contentDescription = "Ligar", tint = MaxGreenSuccess)
                }
                IconButton(onClick = onWhatsApp) {
                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp", tint = Color(0xFF25D366))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Excluir", tint = Color(0xFFFF8A80))
                }
            }
        }
    }
}

@Composable
fun CentralMaxDispatchSection(
    userName: String,
    isAuthenticated: Boolean,
    cloudRepo: MaxSegCloudRepository,
    sosAlerts: List<SosAlert>,
    onAlertDispatched: (SosAlert) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDispatching by remember { mutableStateOf(false) }
    var currentCoords by remember { mutableStateOf(LocationHelper.getCurrentCoordinates(context)) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // TACTICAL SOS BUTTON CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2B0B0B)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color.Red, MaxGold)))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "CENTRAL MAX SEG APIAHY 24 HORAS",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Text(
                    "Ao acionar o botão SOS, sua localização GPS em Apiaí é transmitida para o despacho da Central 24h e para a Viatura V-02 Resgate APH.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )

                Surface(
                    color = Color.Black.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            "📍 Coordenadas de Monitoramento:",
                            color = MaxGold,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${currentCoords.addressEstimate} (Lat: ${"%.4f".format(currentCoords.latitude)}, Lng: ${"%.4f".format(currentCoords.longitude)})",
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                }

                Button(
                    onClick = {
                        isDispatching = true
                        currentCoords = LocationHelper.getCurrentCoordinates(context)
                        scope.launch {
                            if (isAuthenticated) {
                                val res = cloudRepo.dispatchSosAlert(
                                    latitude = currentCoords.latitude,
                                    longitude = currentCoords.longitude,
                                    address = currentCoords.addressEstimate,
                                    notes = "Acionamento de Emergência via App Max Seg por $userName"
                                )
                                isDispatching = false
                                if (res.isSuccess) {
                                    val alert = res.getOrThrow()
                                    onAlertDispatched(alert)
                                } else {
                                    Toast.makeText(context, "Alerta simulado despachado!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                isDispatching = false
                                Toast.makeText(context, "Sinal SOS transmitido em modo local!", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isDispatching,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_trigger_cloud_sos")
                ) {
                    if (isDispatching) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "DISPARAR ALERTA SOS COM GPS",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // DIRECT COMMUNICATION CHANNELS
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaxDarkSurface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Comunicação Direta com o Plantão",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { LocationHelper.openDialer(context, "(15) 99700-0000") },
                        colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundyDark),
                        modifier = Modifier.weight(1f).testTag("btn_call_central")
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ligar 24h", color = Color.White, fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            LocationHelper.openWhatsAppEmergency(
                                context,
                                "(15) 99700-0000",
                                userName,
                                currentCoords.latitude,
                                currentCoords.longitude
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        modifier = Modifier.weight(1f).testTag("btn_whatsapp_central")
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WhatsApp SOS", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // SOS DISPATCH HISTORY
        if (sosAlerts.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaxDarkSurface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Histórico de Despachos na Nuvem",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    sosAlerts.take(3).forEach { alert ->
                        Surface(
                            color = MaxDarkBackground,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "🚨 ${alert.dispatchedUnit}",
                                        color = MaxGold,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "GPS: ${"%.4f".format(alert.latitude)}, ${"%.4f".format(alert.longitude)}",
                                        color = MaxTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Surface(
                                    color = Color(0xFF1B5E20),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        alert.status,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

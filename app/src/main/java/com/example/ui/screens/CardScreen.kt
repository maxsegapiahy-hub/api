package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.model.CardTier
import com.example.model.MemberCardData
import androidx.compose.ui.platform.testTag
import com.example.auth.AuthManager
import com.example.data.MaxSegCloudRepository
import com.example.ui.components.ApiaiInteractiveMap
import com.example.ui.components.SpartanBadgeVector
import com.example.ui.theme.*
import com.example.util.GpsCoordinates
import com.example.util.LocationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardScreen(
    modifier: Modifier = Modifier,
    onNavigateToAccount: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cloudRepo = remember { MaxSegCloudRepository(context) }
    val currentUser by AuthManager.instance.authStateFlow.collectAsState(initial = AuthManager.currentUser)

    var cardData by remember { mutableStateOf(MemberCardData()) }

    // Sync cloud profile to VIP card in real time
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid
        if (uid != null) {
            cloudRepo.observeUserProfile(uid).collect { profile ->
                if (profile != null) {
                    cardData = cardData.copy(
                        memberName = profile.fullName,
                        memberId = profile.cardCode,
                        emergencyPhone = profile.phone,
                        bloodType = profile.bloodType,
                        city = profile.city
                    )
                }
            }
        }
    }
    var isFlipped by remember { mutableStateOf(false) }
    var showMockupDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showTechSpecsDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showMockupDialog) {
        showMockupDialog = false
    }
    BackHandler(enabled = showEditDialog) {
        showEditDialog = false
    }
    BackHandler(enabled = showTechSpecsDialog) {
        showTechSpecsDialog = false
    }
    var isSosActive by remember { mutableStateOf(false) }
    var sosCountdown by remember { mutableIntStateOf(5) }
    var sosDispatched by remember { mutableStateOf(false) }
    var lastGpsCoordinates by remember { mutableStateOf(LocationHelper.getCurrentCoordinates(context)) }

    // SOS Countdown and trigger effect
    LaunchedEffect(isSosActive) {
        if (isSosActive) {
            sosCountdown = 3
            sosDispatched = false
            while (sosCountdown > 0) {
                delay(1000)
                sosCountdown -= 1
            }
            sosDispatched = true
            lastGpsCoordinates = LocationHelper.getCurrentCoordinates(context)
            if (AuthManager.currentUser != null) {
                cloudRepo.dispatchSosAlert(
                    latitude = lastGpsCoordinates.latitude,
                    longitude = lastGpsCoordinates.longitude,
                    address = lastGpsCoordinates.addressEstimate,
                    notes = "Alerta SOS disparado via Cartão VIP por ${cardData.memberName}"
                )
            }
        }
    }

    // Card 3D Flip animation
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaxDarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Account & Cloud Sync Banner
        item {
            Surface(
                onClick = onNavigateToAccount,
                color = if (currentUser != null) Color(0xFF0F2B14) else Color(0xFF261D12),
                shape = RoundedCornerShape(10.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        if (currentUser != null) listOf(MaxGreenSuccess, Color(0xFF2E7D32))
                        else listOf(MaxGold, Color(0xFF8B4513))
                    )
                ),
                modifier = Modifier.fillMaxWidth().testTag("banner_cloud_sync")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = if (currentUser != null) MaxGreenSuccess else MaxGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = if (currentUser != null) "Nuvem Max Seg: Conectado (${currentUser?.email ?: "Associado"})" else "Modo Offline: Perfil Salvo no Dispositivo",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (currentUser != null) "Contatos e histórico SOS sincronizados em tempo real" else "Toque para acessar sua Conta Max e enviar à nuvem",
                                color = MaxTextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Abrir Conta",
                        tint = if (currentUser != null) MaxGreenSuccess else MaxGold,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxBurgundyLight, MaxGold)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CARTÃO DE MEMBRO VIP",
                            color = MaxGold,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Clube Max Vantagens Apiahy",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Toque no cartão para girar (Frente e Verso)",
                            color = MaxTextMuted,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(
                        onClick = { showMockupDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaxBurgundy)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Ver Mockup Real",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Photorealistic Mockup Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clickable { showMockupDialog = true },
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxBurgundy, MaxGoldDark)))
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.img_card_mockup),
                        contentDescription = "Mockup do Cartão de Membro Max Seg",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)),
                                    startY = 100f
                                )
                            )
                    )
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = MaxGold,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "RENDER 3D OFICIAL",
                                color = Color.Black,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Toque para ampliar arte em alta resolução",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Interactive 3D Digital Wallet Member Card
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12f * density
                        }
                        .clickable { isFlipped = !isFlipped },
                    contentAlignment = Alignment.Center
                ) {
                    if (rotation <= 90f) {
                        // FRONT OF CARD
                        MemberCardFront(cardData = cardData)
                    } else {
                        // BACK OF CARD (Flipped horizontally so text is right-side up)
                        Box(
                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                        ) {
                            MemberCardBack(
                                cardData = cardData,
                                onTriggerSos = { isSosActive = true }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilledTonalButton(
                        onClick = { isFlipped = !isFlipped },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaxDarkCardElevated,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.RotateRight, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFlipped) "Ver Frente" else "Ver Verso")
                    }

                    FilledTonalButton(
                        onClick = { showEditDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaxDarkCardElevated,
                            contentColor = MaxGold
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Personalizar")
                    }

                    FilledTonalButton(
                        onClick = { showTechSpecsDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaxDarkCardElevated,
                            contentColor = MaxSilver
                        )
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ficha Gráfica")
                    }
                }
            }
        }

        // Live Emergency SOS 24h Panic Button Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isSosActive) MaxBurgundyDark else MaxDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(
                        listOf(
                            if (isSosActive) Color.Red else MaxBurgundyLight,
                            if (isSosActive) Color.Yellow else MaxRedAccent
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(
                                    text = "SISTEMA SOS 24H",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Simulador de Emergência Médica e Pânico",
                                    color = MaxTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (isSosActive) {
                            Surface(
                                color = Color.Red,
                                shape = CircleShape
                            ) {
                                Text(
                                    text = "ALERTA ATIVO",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (!isSosActive) {
                        Button(
                            onClick = { isSosActive = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaxRedAccent,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "TESTAR BOTÃO DE PÂNICO SOS 24H",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        // SOS Activated State
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!sosDispatched) {
                                Text(
                                    text = "TRANSMITINDO SINAL PARA A CENTRAL...",
                                    color = Color.Yellow,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$sosCountdown",
                                    color = Color.White,
                                    fontSize = 42.sp,
                                    fontWeight = FontWeight.Black
                                )
                            } else {
                                Surface(
                                    color = Color(0xFF1B5E20),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.Green)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "VIATURA DEDICADA DESPACHADA!",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        }
                                        Text(
                                            text = "• Localização GPS: ${lastGpsCoordinates.addressEstimate} (Tempo est.: 4 min)",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "• Socorrista APH: Viatura V-02 em deslocamento com sirene",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = "• Contatos de emergência e Central Max Seg 24h acionados",
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 12.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { LocationHelper.openDialer(context, "(15) 99700-0000") },
                                                colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundyDark),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Ligar 24h", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    LocationHelper.openWhatsAppEmergency(
                                                        context,
                                                        "(15) 99700-0000",
                                                        cardData.memberName,
                                                        lastGpsCoordinates.latitude,
                                                        lastGpsCoordinates.longitude
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("WhatsApp", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                // LIVE TACTICAL DISPATCH MAP
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(240.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color.Red, MaxGold)))
                                ) {
                                    ApiaiInteractiveMap(
                                        modifier = Modifier.fillMaxSize(),
                                        showSosDispatchMode = true
                                    )
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    isSosActive = false
                                    Toast.makeText(context, "Simulação de SOS encerrada", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(Color.Gray, Color.White)))
                            ) {
                                Text("Cancelar / Resetar Alerta")
                            }
                        }
                    }
                }
            }
        }

        // Tier Selection Tabs for Card
        item {
            Text(
                text = "Selecione o Plano do Cartão:",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CardTier.entries.forEach { tier ->
                    val isSelected = cardData.tier == tier
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { cardData = cardData.copy(tier = tier) },
                        color = if (isSelected) Color(tier.primaryColorHex) else MaxDarkCard,
                        shape = RoundedCornerShape(8.dp),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxGold, Color.White))) else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = tier.name,
                                color = if (isSelected) Color.White else MaxTextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    // High-Res Mockup Image Dialog
    if (showMockupDialog) {
        Dialog(onDismissRequest = { showMockupDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaxDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Mockup Oficial do Cartão",
                            color = MaxGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showMockupDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Image(
                        painter = painterResource(id = R.drawable.img_card_mockup),
                        contentDescription = "Visual do Cartão de Membro Max Seg",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Design 'Premium Black' com relevo no Elmo Espartano, letras douradas e selo do plano. Padrão PVC 0.76mm de alta durabilidade para carteira física e versão digital para celular.",
                        color = MaxTextSecondary,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showMockupDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar Visualizador")
                    }
                }
            }
        }
    }

    // Edit Member Data Dialog
    if (showEditDialog) {
        var tempName by remember { mutableStateOf(cardData.memberName) }
        var tempId by remember { mutableStateOf(cardData.memberId) }
        var tempPhone by remember { mutableStateOf(cardData.emergencyPhone) }

        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaxDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Personalizar Cartão de Membro",
                        color = MaxGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Nome do Associado") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempId,
                        onValueChange = { tempId = it },
                        label = { Text("Número de Matrícula / ID") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = tempPhone,
                        onValueChange = { tempPhone = it },
                        label = { Text("Telefone de Emergência") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("Cancelar", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                cardData = cardData.copy(
                                    memberName = tempName.uppercase(),
                                    memberId = tempId,
                                    emergencyPhone = tempPhone
                                )
                                showEditDialog = false
                                Toast.makeText(context, "Cartão atualizado com sucesso!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy)
                        ) {
                            Text("Salvar")
                        }
                    }
                }
            }
        }
    }

    // Technical Specs Dialog for Printing
    if (showTechSpecsDialog) {
        Dialog(onDismissRequest = { showTechSpecsDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaxDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Especificações Gráficas do Cartão",
                            color = MaxGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { showTechSpecsDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }

                    Text("• Material: PVC Black de Massa ou Cristal", color = Color.White, fontSize = 12.sp)
                    Text("• Dimensões: 86mm x 54mm (Padrão ISO ID-1)", color = Color.White, fontSize = 12.sp)
                    Text("• Espessura: 0,76mm (Padrão Cartão Bancário)", color = Color.White, fontSize = 12.sp)
                    Text("• Acabamento: Laminação Fosca (Matte) + Verniz Localizado UV no Elmo", color = Color.White, fontSize = 12.sp)
                    Text("• Cores CMYK Bordô: C:30 M:100 Y:100 K:40", color = MaxGoldLight, fontSize = 12.sp)
                    Text("• Cores CMYK Preto Rico: C:60 M:40 Y:40 K:100", color = MaxGoldLight, fontSize = 12.sp)
                    Text("• Recursos: QR Code de validação + Chip decorativo / NFC", color = Color.White, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { showTechSpecsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Entendido")
                    }
                }
            }
        }
    }
}

@Composable
fun MemberCardFront(
    cardData: MemberCardData,
    modifier: Modifier = Modifier
) {
    val tierColor = Color(cardData.tier.primaryColorHex)
    val accentColor = Color(cardData.tier.accentColorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = MaxBurgundy.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaxDarkCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F0F14),
                            Color(0xFF1E1E28),
                            tierColor.copy(alpha = 0.45f)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.sweepGradient(
                        listOf(accentColor, Color.White, tierColor, accentColor)
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(16.dp)
        ) {
            // Background subtle badge
            SpartanBadgeVector(
                size = 140.dp,
                metalColor = accentColor.copy(alpha = 0.15f),
                baseColor = Color.Transparent,
                accentGleam = false,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 20.dp)
            )

            // Top Header: Logo + Plan Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SpartanBadgeVector(
                        size = 38.dp,
                        metalColor = accentColor,
                        baseColor = MaxBurgundyDark
                    )
                    Column {
                        Text(
                            text = "MAXSEG APIAHY",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "CLUBE DE BENEFÍCIOS",
                            color = accentColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(6.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(accentColor, Color.White)))
                ) {
                    Text(
                        text = cardData.tier.title.uppercase(),
                        color = accentColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Chip & Contactless icon
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gold smart chip
                Box(
                    modifier = Modifier
                        .size(34.dp, 26.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Brush.linearGradient(listOf(MaxGold, MaxGoldDark)))
                        .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                )
                Icon(
                    imageVector = Icons.Default.Contactless,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Bottom Area: Member Name, ID, Expiration
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
            ) {
                Text(
                    text = cardData.memberName,
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID: ${cardData.memberId}",
                        color = MaxTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "VALIDADE: ${cardData.validThru}",
                        color = MaxTextSecondary,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = cardData.city,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MemberCardBack(
    cardData: MemberCardData,
    onTriggerSos: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tierColor = Color(cardData.tier.primaryColorHex)
    val accentColor = Color(cardData.tier.accentColorHex)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(210.dp)
            .shadow(16.dp, RoundedCornerShape(16.dp), spotColor = MaxBurgundy.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaxDarkCard)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0A0A0E),
                            Color(0xFF14141E)
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = accentColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Magnetic Stripe
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .background(Color.Black)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Content area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: QR Code & Validation
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Surface(
                            color = Color.White,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.size(65.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCode2,
                                    contentDescription = "QR Code de Validação",
                                    tint = Color.Black,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                        Text(
                            text = "STATUS: ATIVO",
                            color = MaxGreenSuccess,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Center: Terms & Guidelines
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "ESTE CARTÃO É PESSOAL E INTRANSFERÍVEL",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Apresente nas farmácias, mercados e lojas credenciadas de Apiaí para obter descontos exclusivos.",
                            color = MaxTextMuted,
                            fontSize = 8.sp,
                            lineHeight = 10.sp
                        )
                        Text(
                            text = "Central 24h: ${cardData.emergencyPhone}",
                            color = MaxGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Right: SOS Panic Action Button
                    Surface(
                        onClick = onTriggerSos,
                        color = MaxRedAccent,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.size(width = 75.dp, height = 65.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Emergency,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "ACIONAR",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "SOS 24H",
                                color = Color.Yellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaxBurgundyDark)
                        .padding(vertical = 4.dp, horizontal = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MAX SEG APIAHY - CNPJ 00.000.000/0001-00",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 7.5.sp
                        )
                        Text(
                            text = "APIAÍ - SP",
                            color = MaxGold,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

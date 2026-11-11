package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MaxSegDataRepository
import com.example.model.PinGraduation
import com.example.ui.components.SpartanBadgeVector
import com.example.ui.theme.*

enum class CareerTab(val title: String, val icon: ImageVector) {
    SIMULATOR("Simulador de Ganhos", Icons.Default.MonetizationOn),
    PINS("Bottons & Graduações", Icons.Default.MilitaryTech)
}

/**
 * Tela unificada de Carreira, Comissões e Bottons de Reconhecimento.
 * Elimina duplicidade entre visualização de carreira e exibição dos pins espartanos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CareerScreen(
    modifier: Modifier = Modifier,
    initialTab: CareerTab = CareerTab.SIMULATOR
) {
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    // Simulator states
    var directReferrals by remember { mutableFloatStateOf(10f) }
    var level2PerPerson by remember { mutableFloatStateOf(3f) }
    var level3PerPerson by remember { mutableFloatStateOf(2f) }

    // Computations based on Max Total plan (R$ 199.90: N1=R$30, N2=R$10, N3=R$4)
    val totalDirect = directReferrals.toInt()
    val totalLevel2 = (totalDirect * level2PerPerson).toInt()
    val totalLevel3 = (totalLevel2 * level3PerPerson).toInt()

    val incomeDirect = totalDirect * 30.0
    val incomeLevel2 = totalLevel2 * 10.0
    val incomeLevel3 = totalLevel3 * 4.0
    val totalMonthlyIncome = incomeDirect + incomeLevel2 + incomeLevel3

    val totalPoints = (totalDirect + totalLevel2 + totalLevel3) * 200

    val calculatedRank = when {
        totalPoints >= 80000 -> "DIAMANTE"
        totalPoints >= 30000 -> "OURO"
        totalPoints >= 10000 -> "PRATA"
        totalPoints >= 3000 -> "BRONZE"
        totalDirect >= 1 -> "AFILIADO"
        else -> "INICIANTE"
    }

    val rankColor = when (calculatedRank) {
        "DIAMANTE" -> Color(0xFF80D8FF)
        "OURO" -> MaxGold
        "PRATA" -> MaxSilver
        "BRONZE" -> MaxBronze
        else -> MaxBurgundyLight
    }

    // Pins data & dialogs
    val pins = remember { MaxSegDataRepository.pinsGraduations }
    var selectedPin by remember { mutableStateOf(pins.first()) }
    var showTechGuideDialog by remember { mutableStateOf(false) }
    var showZoomPinDialog by remember { mutableStateOf<PinGraduation?>(null) }

    BackHandler(enabled = showZoomPinDialog != null) {
        showZoomPinDialog = null
    }
    BackHandler(enabled = showTechGuideDialog) {
        showTechGuideDialog = false
    }
    BackHandler(enabled = selectedTab == CareerTab.PINS) {
        selectedTab = CareerTab.SIMULATOR
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaxDarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Main Unified Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(MaxBurgundyLight, MaxGold))
                )
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
                            text = "PLANO DE CARREIRA & AFILIADOS",
                            color = MaxGold,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Social Selling Sustentável & Graduações Espartanas",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Ganhos em 3 níveis com reconhecimento físico oficial",
                            color = MaxTextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
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
                }
            }
        }

        // Sub-Navigation Tabs
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
                CareerTab.entries.forEach { tab ->
                    val isSelected = selectedTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (isSelected) MaxGold else MaxTextMuted
                                )
                                Text(
                                    tab.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaxGold else MaxTextSecondary
                                )
                            }
                        }
                    )
                }
            }
        }

        // TAB 1: SIMULADOR DE GANHOS
        if (selectedTab == CareerTab.SIMULATOR) {
            // Live Calculated Projection Hero Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxDarkCardElevated),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(MaxGreenSuccess, MaxBurgundyDark))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "RENDA RECORRENTE MENSAL ESTIMADA",
                            color = MaxTextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "R$ ${"%,.2f".format(totalMonthlyIncome)}",
                            color = MaxGreenSuccess,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "Ganhos todo mês depositados na sua conta",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = rankColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(listOf(rankColor, Color.White))
                                )
                            ) {
                                Text(
                                    text = "GRADUAÇÃO: $calculatedRank",
                                    color = rankColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Surface(
                                color = MaxDarkCard,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "$totalPoints PV Acumulados",
                                    color = MaxGold,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Sliders Section
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Ajuste sua Estrutura de Indicações:",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Direct slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Indicações Diretas (Nível 1):", color = MaxTextSecondary, fontSize = 12.sp)
                                Text("$totalDirect clientes (R$ ${"%.0f".format(incomeDirect)}/mês)", color = MaxGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = directReferrals,
                                onValueChange = { directReferrals = it },
                                valueRange = 0f..50f,
                                steps = 49,
                                colors = SliderDefaults.colors(thumbColor = MaxGold, activeTrackColor = MaxGold)
                            )
                        }

                        // Level 2 slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Média de indicações de cada direto (Nível 2):", color = MaxTextSecondary, fontSize = 12.sp)
                                Text("$totalLevel2 clientes (R$ ${"%.0f".format(incomeLevel2)}/mês)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = level2PerPerson,
                                onValueChange = { level2PerPerson = it },
                                valueRange = 0f..10f,
                                steps = 9,
                                colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White)
                            )
                        }

                        // Level 3 slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Média de indicações do Nível 3:", color = MaxTextSecondary, fontSize = 12.sp)
                                Text("$totalLevel3 clientes (R$ ${"%.0f".format(incomeLevel3)}/mês)", color = MaxTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = level3PerPerson,
                                onValueChange = { level3PerPerson = it },
                                valueRange = 0f..5f,
                                steps = 4,
                                colors = SliderDefaults.colors(thumbColor = MaxSilver, activeTrackColor = MaxSilver)
                            )
                        }
                    }
                }
            }

            // Sustainability Rules
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxBurgundyDark.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(MaxBurgundyLight, Color.Transparent))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaxGold)
                            Text(
                                text = "Pilares de Sustentabilidade Matemática",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text("1. Teto Máximo de Repasse: A soma de todas as comissões nunca ultrapassa 25-30% da mensalidade.", color = MaxTextSecondary, fontSize = 12.sp)
                        Text("2. Regra de Ativação Mensal: O afiliado só recebe comissão se mantiver seu plano ativo e em dia.", color = MaxTextSecondary, fontSize = 12.sp)
                        Text("3. Sem Taxas de Adesão Ocultas: O dinheiro vem do pagamento real de serviços essenciais prestados.", color = MaxTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // TAB 2: BOTTONS & GRADUAÇÕES
        if (selectedTab == CareerTab.PINS) {
            // Horizontal Pins Selector Ribbon
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pins Oficiais de Graduação:",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    IconButton(
                        onClick = { showTechGuideDialog = true },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaxDarkCardElevated)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Engineering,
                            contentDescription = "Guia de Fabricação",
                            tint = MaxGold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pins) { pin ->
                        val isSelected = pin.id == selectedPin.id
                        val metalColor = Color(pin.metalColorHex)

                        Surface(
                            modifier = Modifier
                                .width(100.dp)
                                .clickable { selectedPin = pin },
                            color = if (isSelected) MaxDarkCardElevated else MaxDarkCard,
                            shape = RoundedCornerShape(14.dp),
                            border = if (isSelected) {
                                CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.verticalGradient(listOf(metalColor, Color.White))
                                )
                            } else null
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(CircleShape)
                                        .border(1.5.dp, metalColor, CircleShape)
                                ) {
                                    Image(
                                        painter = painterResource(id = pin.imageRes),
                                        contentDescription = pin.rankName,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                Text(
                                    text = pin.rankName,
                                    color = if (isSelected) metalColor else MaxTextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            // Selected Pin Detailed Hero Showcase
            item {
                val metalColor = Color(selectedPin.metalColorHex)

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                    shape = RoundedCornerShape(20.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(
                            listOf(metalColor, MaxBurgundyDark)
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Title badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "NÍVEL ${selectedPin.rankName}",
                                    color = metalColor,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = selectedPin.rankTitle,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                color = metalColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = CardDefaults.outlinedCardBorder().copy(
                                    brush = Brush.horizontalGradient(listOf(metalColor, Color.White))
                                )
                            ) {
                                Text(
                                    text = "${selectedPin.requiredPoints} PV",
                                    color = metalColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Large Pin Photo with Zoom Trigger
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .shadow(24.dp, CircleShape, spotColor = metalColor.copy(alpha = 0.7f))
                                .clip(CircleShape)
                                .border(3.dp, Brush.sweepGradient(listOf(metalColor, Color.White, metalColor)), CircleShape)
                                .clickable { showZoomPinDialog = selectedPin }
                        ) {
                            Image(
                                painter = painterResource(id = selectedPin.imageRes),
                                contentDescription = selectedPin.rankName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(Color.Black.copy(alpha = 0.6f))
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    text = "Toque p/ Ampliar",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "\"${selectedPin.meaning}\"",
                            color = MaxTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = MaxDarkCardElevated)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Specifications Matrix
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CareerPinInfoRow(icon = Icons.Default.Shield, label = "Acabamento Metálico:", value = selectedPin.metalFinish)
                            CareerPinInfoRow(icon = Icons.Default.Palette, label = "Esmaltação:", value = selectedPin.enamelColor)
                            CareerPinInfoRow(icon = Icons.Default.Stars, label = "Detalhe Exclusivo:", value = selectedPin.accentDetail)
                            CareerPinInfoRow(icon = Icons.Default.Group, label = "Volume de Ativação:", value = selectedPin.estimatedActivePlans)
                            CareerPinInfoRow(icon = Icons.Default.MonetizationOn, label = "Comissão Direta (N1):", value = selectedPin.directCommission, valueColor = MaxGreenSuccess)
                            if (selectedPin.indirectLevel2 != "-") {
                                CareerPinInfoRow(icon = Icons.Default.AccountTree, label = "Comissão Nível 2:", value = selectedPin.indirectLevel2)
                            }
                            if (selectedPin.indirectLevel3 != "-") {
                                CareerPinInfoRow(icon = Icons.Default.AccountTree, label = "Comissão Nível 3:", value = selectedPin.indirectLevel3)
                            }
                            CareerPinInfoRow(icon = Icons.Default.CardGiftcard, label = "Prêmio & Bônus:", value = selectedPin.specialBonus, valueColor = MaxGold)
                        }
                    }
                }
            }

            // Manufacturing Golden Rule Callout
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxBurgundyDark),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(MaxGold, MaxBurgundyLight))
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaxGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Column {
                            Text(
                                text = "Dica de Ouro de Fabricação",
                                color = MaxGold,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Economize mais de 60% fabricando apenas 1 molde de relevo (Zamac 3D) e aplicando apenas banhos químicos diferentes (Cobre, Prata, Ouro, Níquel Preto).",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Zoom Pin Dialog
    showZoomPinDialog?.let { pin ->
        val metalColor = Color(pin.metalColorHex)
        Dialog(onDismissRequest = { showZoomPinDialog = null }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaxDarkCard)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "PIN GRADUAÇÃO ${pin.rankName}",
                            color = metalColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(onClick = { showZoomPinDialog = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .clip(CircleShape)
                            .border(3.dp, metalColor, CircleShape)
                    ) {
                        Image(
                            painter = painterResource(id = pin.imageRes),
                            contentDescription = pin.rankName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Text(
                        text = "${pin.rankTitle} • ${pin.metalFinish}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )

                    Button(
                        onClick = { showZoomPinDialog = null },
                        colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }

    // Technical Guide Dialog
    if (showTechGuideDialog) {
        Dialog(onDismissRequest = { showTechGuideDialog = false }) {
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
                            text = "Ficha Técnica para Produção de PINS",
                            color = MaxGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        IconButton(onClick = { showTechGuideDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                        }
                    }

                    Text("• Material Base: Liga metálica Zamac 5 fundida sob pressão 3D", color = Color.White, fontSize = 12.sp)
                    Text("• Dimensão: 25mm a 30mm diâmetro x 2.0mm a 2.5mm espessura", color = Color.White, fontSize = 12.sp)
                    Text("• Fecho: Tarraxa Borboleta metálica dupla com pino de fixação anti-giro", color = Color.White, fontSize = 12.sp)
                    Text("• Esmaltação: Soft Enamel automotivo ou Resina Epóxi UV", color = Color.White, fontSize = 12.sp)
                    Text("• Banho Químico (Galvanoplastia): Cobre envelhecido, Níquel brilhante, Ouro 24k e Black Nickel", color = MaxGoldLight, fontSize = 12.sp)
                    Text("• Embalagem: Saquinho de veludo preto gravado em hot-stamping dourado", color = Color.White, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(6.dp))

                    Button(
                        onClick = { showTechGuideDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar Guia")
                    }
                }
            }
        }
    }
}

@Composable
private fun CareerPinInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaxGold,
            modifier = Modifier
                .size(16.dp)
                .padding(top = 2.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = MaxTextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = valueColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

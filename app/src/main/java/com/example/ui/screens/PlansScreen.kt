package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MaxSegDataRepository
import com.example.model.PlanOffer
import com.example.ui.theme.*

enum class PlansTab(val title: String, val icon: ImageVector) {
    PLANS("Planos & Preços", Icons.Default.Shield),
    SCRIPTS("Scripts de Vendas", Icons.Default.Article)
}

/**
 * Tela unificada de Planos, Coberturas e Scripts de Fechamento Comercial.
 * Elimina duplicidade entre visualização de planos e scripts de vendas dos mesmos planos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlansScreen(
    modifier: Modifier = Modifier,
    initialTab: PlansTab = PlansTab.PLANS
) {
    val context = LocalContext.current
    val plans = remember { MaxSegDataRepository.plansOffers }
    val pitches = remember { MaxSegDataRepository.commercialPitches }

    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }
    var selectedPitch by remember { mutableStateOf(pitches.first()) }

    BackHandler(enabled = selectedTab == PlansTab.SCRIPTS) {
        selectedTab = PlansTab.PLANS
    }

    fun copyToClipboard(text: String, label: String = "Texto") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
    }

    fun shareWhatsApp(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar Proposta via"))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaxDarkBackground)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // Top Banner
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                shape = RoundedCornerShape(16.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(MaxBurgundyLight, MaxGold))
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "PLANOS & SCRIPTS COMERCIAIS",
                        color = MaxGold,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Cinturão de Proteção, Saúde & Abordagens de Vendas",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Preços calculados para Apiaí e roteiros prontos para fechamento",
                        color = MaxTextMuted,
                        fontSize = 11.sp
                    )
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
                PlansTab.entries.forEach { tab ->
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

        // TAB 1: PLANOS & PREÇOS
        if (selectedTab == PlansTab.PLANS) {
            items(plans) { plan ->
                PlanCardItem(plan = plan)
            }

            // Financial & Telemedicine Margin Calculator Callout
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(MaxGreenSuccess, MaxBlueInfo))
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
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = MaxGreenSuccess
                            )
                            Text(
                                text = "Análise de Margem da Telemedicina 24h",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "• Custo de compra atacado no convênio: ~R$ 10,00 por vida ativa",
                            color = MaxTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• Preço de venda no Clube Max Vantagens: R$ 39,90/mês",
                            color = MaxTextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "• Lucro bruto por membro: R$ 29,90/mês (quase 300% de margem)",
                            color = MaxGreenSuccess,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "• Sustenta a comissão de 3 níveis dos afiliados com sobra limpa para o caixa de Apiaí.",
                            color = MaxTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // TAB 2: SCRIPTS DE VENDAS
        if (selectedTab == PlansTab.SCRIPTS) {
            // Horizontal Category Tabs
            item {
                Text(
                    text = "Escolha a Abordagem Comercial:",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(pitches) { pitch ->
                        val isSelected = pitch.id == selectedPitch.id
                        Surface(
                            onClick = { selectedPitch = pitch },
                            color = if (isSelected) MaxBurgundy else MaxDarkCard,
                            shape = RoundedCornerShape(10.dp),
                            border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(MaxGold, Color.White))
                            ) else null
                        ) {
                            Text(
                                text = pitch.title.replace("Abordagem para ", "").replace("Script Completo para o ", ""),
                                color = if (isSelected) Color.White else MaxTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            // Active Pitch Script Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(MaxGoldDark, MaxBurgundyDark))
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = selectedPitch.title,
                            color = MaxGold,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Público Alvo: ${selectedPitch.targetAudience}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Surface(
                            color = MaxDarkCardElevated,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text("Gatilhos & Argumentos Chave:", color = MaxGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                selectedPitch.keyHooks.forEach { hook ->
                                    Row(
                                        verticalAlignment = Alignment.Top,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text("•", color = MaxGold, fontSize = 12.sp)
                                        Text(hook, color = MaxTextSecondary, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Text("Roteiro Verbal para Apresentação:", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Surface(
                            color = Color(0xFF0F0F14),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedPitch.pitchBody,
                                color = MaxTextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { copyToClipboard(selectedPitch.pitchBody, "Roteiro") },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaxDarkCardElevated,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copiar Fala", fontSize = 12.sp)
                            }

                            Button(
                                onClick = { shareWhatsApp(selectedPitch.whatsappMessage) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // WhatsApp Proposal Box
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Texto Formal para WhatsApp / Mensagem:",
                                color = MaxGold,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { copyToClipboard(selectedPitch.whatsappMessage, "Mensagem WhatsApp") }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", tint = Color.White)
                            }
                        }

                        Surface(
                            color = Color(0xFF0F0F14),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = selectedPitch.whatsappMessage,
                                color = MaxTextSecondary,
                                fontSize = 11.5.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanCardItem(plan: PlanOffer) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (plan.isPopular) MaxBurgundyDark.copy(alpha = 0.6f) else MaxDarkCard),
        shape = RoundedCornerShape(16.dp),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = if (plan.isPopular) Brush.horizontalGradient(listOf(MaxGold, MaxRedAccent)) else Brush.horizontalGradient(listOf(MaxBurgundyLight.copy(alpha = 0.5f), Color.Transparent))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.subtitle,
                        color = MaxTextSecondary,
                        fontSize = 12.sp
                    )
                }

                Surface(
                    color = if (plan.isPopular) MaxGold else MaxDarkCardElevated,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = plan.priceFormatted,
                        color = if (plan.isPopular) Color.Black else MaxGold,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Surface(
                color = if (plan.isPopular) MaxRedAccent else MaxBurgundyDark,
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = plan.categoryBadge,
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            HorizontalDivider(color = MaxDarkCardElevated)

            // Features list
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                plan.features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = if (plan.isPopular) MaxGold else MaxGreenSuccess,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = feature,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = MaxDarkCardElevated)

            // Commissions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Comissão Nível 1:", color = MaxTextMuted, fontSize = 10.sp)
                    Text("R$ ${"%.2f".format(plan.commissionDirect)}/mês", color = MaxGreenSuccess, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Comissão Nível 2:", color = MaxTextMuted, fontSize = 10.sp)
                    Text("R$ ${"%.2f".format(plan.commissionLevel2)}/mês", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Comissão Nível 3:", color = MaxTextMuted, fontSize = 10.sp)
                    Text("R$ ${"%.2f".format(plan.commissionLevel3)}/mês", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

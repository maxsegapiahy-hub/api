package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MaxSegCloudRepository
import com.example.data.MaxSegDataRepository
import com.example.model.PartnerBusiness
import com.example.model.PartnerStatus
import com.example.ui.components.ApiaiInteractiveMap
import com.example.ui.theme.*
import com.example.util.LocationHelper
import kotlinx.coroutines.launch
import java.text.Normalizer

enum class PartnersViewMode {
    MAP, LIST
}

/**
 * Categoria de tipo de serviço para filtro na rede de parceiros de Apiaí.
 */
data class ServiceCategoryFilter(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val matcher: (PartnerBusiness) -> Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnersScreen(
    modifier: Modifier = Modifier,
    initialMode: PartnersViewMode = PartnersViewMode.LIST
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cloudRepo = remember { MaxSegCloudRepository(context) }

    var partnersList by remember { mutableStateOf(MaxSegDataRepository.initialPartners) }
    var isCloudSynced by remember { mutableStateOf(false) }
    var isLoadingCloud by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf("ALL") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showStickerSpecsDialog by remember { mutableStateOf(false) }
    var selectedPartnerDetail by remember { mutableStateOf<PartnerBusiness?>(null) }
    var viewMode by remember(initialMode) { mutableStateOf(initialMode) }

    // Intercept back button when sub-views or dialogs are active
    BackHandler(enabled = selectedPartnerDetail != null) {
        selectedPartnerDetail = null
    }
    BackHandler(enabled = showAddDialog) {
        showAddDialog = false
    }
    BackHandler(enabled = showStickerSpecsDialog) {
        showStickerSpecsDialog = false
    }
    BackHandler(enabled = viewMode == PartnersViewMode.MAP) {
        viewMode = PartnersViewMode.LIST
    }

    // Seed and observe partners in real-time from Firestore
    LaunchedEffect(Unit) {
        // Step 1: Ensure initial database seed if Firestore collection is empty or missing newly added services
        try {
            cloudRepo.seedInitialPartnersIfEmpty()
        } catch (_: Exception) {
            // Continues to observe
        }

        // Step 2: Live reactive snapshot listener on "partners" collection
        try {
            cloudRepo.observePartners().collect { cloudList ->
                if (cloudList.isNotEmpty()) {
                    partnersList = cloudList
                    isCloudSynced = true
                }
                isLoadingCloud = false
            }
        } catch (_: Exception) {
            isLoadingCloud = false
        }
    }

    // Definição dos tipos de serviço para filtragem estruturada
    val serviceCategories = remember {
        listOf(
            ServiceCategoryFilter("ALL", "Todos", Icons.Default.Storefront) { true },
            ServiceCategoryFilter("PHARMACY", "Farmácias", Icons.Default.LocalPharmacy) { partner ->
                val s = normalizeText(partner.segment)
                val n = normalizeText(partner.businessName)
                s.contains("farmacia") || n.contains("drogaria") || n.contains("farmacia")
            },
            ServiceCategoryFilter("CLINICS", "Clínicas & Saúde", Icons.Default.MedicalServices) { partner ->
                val s = normalizeText(partner.segment)
                val n = normalizeText(partner.businessName)
                s.contains("clinica") || s.contains("saude") || s.contains("laboratorio") ||
                        n.contains("clinica") || n.contains("laboratorio") || n.contains("odont")
            },
            ServiceCategoryFilter("SUPERMARKETS", "Supermercados", Icons.Default.ShoppingCart) { partner ->
                val s = normalizeText(partner.segment)
                val n = normalizeText(partner.businessName)
                s.contains("supermercado") || n.contains("supermercado") || n.contains("mercado")
            },
            ServiceCategoryFilter("GAS_STATIONS", "Postos & Combustível", Icons.Default.LocalGasStation) { partner ->
                val s = normalizeText(partner.segment)
                val n = normalizeText(partner.businessName)
                s.contains("posto") || n.contains("posto")
            },
            ServiceCategoryFilter("FASHION", "Moda & Vestuário", Icons.Default.Checkroom) { partner ->
                val s = normalizeText(partner.segment)
                val n = normalizeText(partner.businessName)
                s.contains("roupa") || s.contains("moda") || n.contains("boutique")
            },
            ServiceCategoryFilter("ELECTRONICS", "Eletro & Móveis", Icons.Default.Weekend) { partner ->
                val s = normalizeText(partner.segment)
                val n = normalizeText(partner.businessName)
                s.contains("eletro") || s.contains("moveis") || n.contains("eletro") || n.contains("moveis")
            },
            ServiceCategoryFilter("PET_SHOP", "Pet Shop & Agro", Icons.Default.Pets) { partner ->
                val s = normalizeText(partner.segment)
                val n = normalizeText(partner.businessName)
                s.contains("pet") || s.contains("racao") || n.contains("agro") || n.contains("pet")
            },
            ServiceCategoryFilter("AUTO", "Oficinas & Autopeças", Icons.Default.Build) { partner ->
                val s = normalizeText(partner.segment)
                val n = normalizeText(partner.businessName)
                s.contains("autopecas") || s.contains("oficina") || n.contains("mecanica") || n.contains("auto pecas")
            }
        )
    }

    val activeCategory = serviceCategories.firstOrNull { it.id == selectedCategoryId } ?: serviceCategories.first()

    // Filtragem avançada por texto (nome, serviço, benefício, bairro) e categoria selecionada
    val filteredPartners = partnersList.filter { partner ->
        val matchesCategory = activeCategory.matcher(partner)

        val matchesSearch = if (searchQuery.isBlank()) {
            true
        } else {
            val queryNormalized = normalizeText(searchQuery)
            val queryTokens = queryNormalized.split(" ").filter { it.isNotBlank() }

            val nameNorm = normalizeText(partner.businessName)
            val segmentNorm = normalizeText(partner.segment)
            val discountNorm = normalizeText(partner.benefitDiscount)
            val neighborhoodNorm = normalizeText(partner.neighborhood)
            val addressNorm = normalizeText(partner.address)
            val contactNorm = normalizeText(partner.contactPerson)

            queryTokens.all { token ->
                nameNorm.contains(token) ||
                        segmentNorm.contains(token) ||
                        discountNorm.contains(token) ||
                        neighborhoodNorm.contains(token) ||
                        addressNorm.contains(token) ||
                        contactNorm.contains(token) ||
                        // Sinônimos inteligentes para busca rápida em Apiaí
                        matchesServiceSynonyms(token, partner)
            }
        }

        matchesCategory && matchesSearch
    }

    val isFilterActive = searchQuery.isNotBlank() || selectedCategoryId != "ALL"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaxDarkBackground)
    ) {
        // Header & View Mode Switcher
        Surface(
            color = MaxDarkCard,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // View Mode Tabs
                    Surface(
                        color = MaxDarkCardElevated,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(3.dp)) {
                            Surface(
                                onClick = { viewMode = PartnersViewMode.LIST },
                                color = if (viewMode == PartnersViewMode.LIST) MaxBurgundy else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("tab_partners_list")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ViewList,
                                        contentDescription = null,
                                        tint = if (viewMode == PartnersViewMode.LIST) MaxGold else MaxTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Lista (${filteredPartners.size})",
                                        color = if (viewMode == PartnersViewMode.LIST) Color.White else MaxTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                onClick = { viewMode = PartnersViewMode.MAP },
                                color = if (viewMode == PartnersViewMode.MAP) MaxBurgundy else Color.Transparent,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("tab_partners_map")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Map,
                                        contentDescription = null,
                                        tint = if (viewMode == PartnersViewMode.MAP) MaxGold else MaxTextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Mapa Apiaí",
                                        color = if (viewMode == PartnersViewMode.MAP) Color.White else MaxTextSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilledIconButton(
                            onClick = { showAddDialog = true },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaxBurgundy),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("add_partner_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Cadastrar Parceiro", tint = Color.White, modifier = Modifier.size(18.dp))
                        }

                        IconButton(
                            onClick = { showStickerSpecsDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(containerColor = MaxDarkCardElevated),
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("sticker_specs_button")
                        ) {
                            Icon(Icons.Default.Label, contentDescription = "Adesivo Oficial", tint = MaxGold, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                // Cloud Status Bar
                Surface(
                    color = MaxDarkCardElevated.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(
                                        if (isCloudSynced) MaxGreenSuccess else MaxGold,
                                        CircleShape
                                    )
                            )
                            Text(
                                text = if (isCloudSynced) {
                                    "maxsegapiahy-hub/api & Firestore (${partnersList.size} parceiros em Apiaí)"
                                } else if (isLoadingCloud) {
                                    "Sincronizando maxsegapiahy-hub/api..."
                                } else {
                                    "Rede de Estabelecimentos de Apiaí-SP"
                                },
                                color = if (isCloudSynced) MaxGreenSuccess else MaxGoldLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    isLoadingCloud = true
                                    cloudRepo.seedInitialPartnersIfEmpty()
                                    val apiResult = cloudRepo.syncPartnersFromApi()
                                    isLoadingCloud = false
                                    if (apiResult.isSuccess && apiResult.getOrNull()?.isNotEmpty() == true) {
                                        Toast.makeText(context, "Sincronizado com API maxsegapiahy-hub e Firestore!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Rede sincronizada com a Nuvem Firestore!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Recarregar",
                                tint = MaxGold,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        if (viewMode == PartnersViewMode.MAP) {
            // FULL INTERACTIVE CANVAS MAP (Reflects active search & service filter)
            ApiaiInteractiveMap(
                modifier = Modifier.fillMaxSize(),
                partners = filteredPartners,
                selectedPartner = selectedPartnerDetail,
                onSelectPartner = { selectedPartnerDetail = it }
            )
        } else {
            // LIST VIEW WITH FULL CONTACT DETAILS & DISCOUNTS
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                // Mini Map Quick Preview Banner
                item {
                    Card(
                        onClick = { viewMode = PartnersViewMode.MAP },
                        colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                        shape = RoundedCornerShape(16.dp),
                        border = CardDefaults.outlinedCardBorder().copy(
                            brush = Brush.horizontalGradient(listOf(MaxBurgundyLight, MaxGold))
                        ),
                        modifier = Modifier.testTag("interactive_map_banner")
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
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(MaxBurgundyDark, RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Explore, contentDescription = null, tint = MaxGold)
                                }
                                Column {
                                    Text(
                                        text = "MAPA INTERATIVO DE APIAÍ",
                                        color = MaxGold,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Ver estabelecimentos e viaturas em tempo real",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                            Button(
                                onClick = { viewMode = PartnersViewMode.MAP },
                                colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Abrir Mapa", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Search Bar with Clear Button
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = {
                            Text("Buscar por nome ou serviço (ex: farmácias, clínicas)...", color = MaxTextMuted, fontSize = 12.sp)
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MaxGold)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.testTag("clear_search_button")
                                ) {
                                    Icon(Icons.Default.Clear, contentDescription = "Limpar busca", tint = MaxGold)
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaxGold,
                            unfocusedBorderColor = MaxDarkCardElevated,
                            focusedContainerColor = MaxDarkCard,
                            unfocusedContainerColor = MaxDarkCard
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_partners_input")
                    )
                }

                // Quick Service Filter Chips Carousel
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TIPO DE SERVIÇO EM APIAÍ",
                                color = MaxGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )

                            if (isFilterActive) {
                                Surface(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategoryId = "ALL"
                                    },
                                    color = MaxDarkCardElevated,
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.testTag("reset_all_filters_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = MaxGold, modifier = Modifier.size(12.dp))
                                        Text("Limpar Filtros", color = MaxGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(serviceCategories, key = { it.id }) { category ->
                                val isSelected = selectedCategoryId == category.id
                                val count = partnersList.count { category.matcher(it) }

                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategoryId = category.id
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = category.icon,
                                            contentDescription = null,
                                            tint = if (isSelected) MaxGold else MaxTextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = if (category.id == "ALL") "${category.label} (${partnersList.size})" else "${category.label} ($count)",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaxBurgundy,
                                        selectedLabelColor = Color.White,
                                        containerColor = MaxDarkCard,
                                        labelColor = MaxTextSecondary
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (isSelected) MaxGold else MaxDarkCardElevated,
                                        selectedBorderColor = MaxGold,
                                        borderWidth = 1.dp,
                                        enabled = true,
                                        selected = isSelected
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("filter_chip_${category.id.lowercase()}")
                                )
                            }
                        }
                    }
                }

                // Results Counter & Active Query Status
                item {
                    Surface(
                        color = MaxDarkCardElevated.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Exibindo ${filteredPartners.size} de ${partnersList.size} estabelecimentos",
                                color = MaxTextSecondary,
                                fontSize = 11.sp
                            )

                            if (selectedCategoryId != "ALL" || searchQuery.isNotBlank()) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) "\"$searchQuery\"" else activeCategory.label,
                                    color = MaxGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Empty state if filter yields zero matches
                if (filteredPartners.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.SearchOff, contentDescription = null, tint = MaxGold, modifier = Modifier.size(40.dp))
                                Text(
                                    text = "Nenhum parceiro encontrado",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Não encontramos estabelecimentos para \"$searchQuery\" na categoria selecionada em Apiaí.",
                                    color = MaxTextMuted,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )

                                Button(
                                    onClick = {
                                        searchQuery = ""
                                        selectedCategoryId = "ALL"
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Ver Todos os Parceiros", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                // Partners List
                items(filteredPartners, key = { it.id }) { partner ->
                    PartnerDetailedCard(
                        partner = partner,
                        onClick = { selectedPartnerDetail = partner },
                        onOpenOnMap = {
                            selectedPartnerDetail = partner
                            viewMode = PartnersViewMode.MAP
                        },
                        onCall = {
                            LocationHelper.openDialer(context, partner.phone)
                        },
                        onWhatsApp = {
                            openWhatsAppPartner(context, partner)
                        },
                        onGoogleMaps = {
                            openGoogleMapsPartner(context, partner)
                        },
                        onToggleStatus = {
                            val nextStatus = when (partner.status) {
                                PartnerStatus.PROSPECCAO -> PartnerStatus.CONFIRMADO
                                PartnerStatus.CONFIRMADO -> PartnerStatus.ATIVO
                                PartnerStatus.ATIVO -> PartnerStatus.PROSPECCAO
                            }
                            // Optimistic UI update
                            partnersList = partnersList.map {
                                if (it.id == partner.id) it.copy(status = nextStatus) else it
                            }
                            // Firestore write
                            coroutineScope.launch {
                                cloudRepo.updatePartnerStatus(partner.id, nextStatus)
                            }
                            Toast.makeText(context, "Status atualizado: ${nextStatus.label}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    // Detail Dialog
    selectedPartnerDetail?.let { partner ->
        if (viewMode == PartnersViewMode.LIST) {
            Dialog(onDismissRequest = { selectedPartnerDetail = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaxDarkCard)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = partner.businessName,
                                    color = MaxGold,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "${partner.segment} • ${partner.neighborhood}",
                                    color = MaxTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                            IconButton(onClick = { selectedPartnerDetail = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                            }
                        }

                        // Prominent Discount Callout
                        Surface(
                            color = MaxDarkCardElevated,
                            shape = RoundedCornerShape(10.dp),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.horizontalGradient(listOf(MaxGold, MaxGreenSuccess))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "DESCONTO / BENEFÍCIO EXCLUSIVO:",
                                    color = MaxGold,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    partner.benefitDiscount,
                                    color = MaxGreenSuccess,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text("• Responsável: ${partner.contactPerson}", color = Color.White, fontSize = 13.sp)
                        Text("• Telefone: ${partner.phone}", color = Color.White, fontSize = 13.sp)
                        Text("• Endereço: ${partner.address}", color = MaxTextSecondary, fontSize = 12.sp)

                        HorizontalDivider(color = MaxDarkCardElevated)

                        Text("Argumento de Abordagem & Apresentação:", color = MaxGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(partner.argumentPitch, color = MaxTextSecondary, fontSize = 12.sp)

                        Spacer(modifier = Modifier.height(6.dp))

                        // Quick Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = {
                                    LocationHelper.openDialer(context, partner.phone)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaxDarkCardElevated),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = MaxGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ligar", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    openWhatsAppPartner(context, partner)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 11.sp, color = Color.White)
                            }

                            Button(
                                onClick = {
                                    openGoogleMapsPartner(context, partner)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Navigation, contentDescription = null, tint = MaxGold, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Maps", fontSize = 11.sp, color = Color.White)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                viewMode = PartnersViewMode.MAP
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaxGold),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Ver no Mapa Interativo de Apiaí")
                        }
                    }
                }
            }
        }
    }

    // Add Partner Dialog (saves directly to Firestore)
    if (showAddDialog) {
        var name by remember { mutableStateOf("") }
        var segment by remember { mutableStateOf("Farmácia") }
        var discount by remember { mutableStateOf("") }
        var contact by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var address by remember { mutableStateOf("") }
        var neighborhood by remember { mutableStateOf("Centro") }
        var isSaving by remember { mutableStateOf(false) }

        val availableSegments = listOf(
            "Farmácia",
            "Clínica & Saúde",
            "Supermercado",
            "Posto de Combustíveis",
            "Loja de Eletrodomésticos",
            "Loja de Roupas / Moda",
            "Pet Shop / Casa de Ração",
            "Laboratório de Exames",
            "Autopeças & Oficina"
        )

        Dialog(onDismissRequest = { if (!isSaving) showAddDialog = false }) {
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
                    Text(
                        text = "Cadastrar Novo Parceiro em Apiaí",
                        color = MaxGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nome do Estabelecimento") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Segment Dropdown or Chips
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Tipo de Serviço / Ramo:", color = MaxTextSecondary, fontSize = 11.sp)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(availableSegments) { seg ->
                                FilterChip(
                                    selected = segment == seg,
                                    onClick = { segment = seg },
                                    label = { Text(seg, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaxBurgundy,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = discount,
                        onValueChange = { discount = it },
                        label = { Text("Desconto / Benefício oferecido") },
                        placeholder = { Text("Ex: 25% de desconto à vista", color = MaxTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = neighborhood,
                        onValueChange = { neighborhood = it },
                        label = { Text("Bairro em Apiaí (Centro, Pinheiros, etc)") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Endereço Completo em Apiaí") },
                        placeholder = { Text("Rua ..., nº - Apiaí/SP", color = MaxTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        label = { Text("Nome do Proprietário / Responsável") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Telefone / WhatsApp") },
                        placeholder = { Text("(15) 3552-0000", color = MaxTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showAddDialog = false },
                            enabled = !isSaving
                        ) {
                            Text("Cancelar", color = Color.White)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (name.isNotBlank()) {
                                    isSaving = true
                                    val newPartner = PartnerBusiness(
                                        id = "partner_${System.currentTimeMillis()}",
                                        segment = segment,
                                        businessName = name,
                                        benefitDiscount = discount.ifBlank { "Desconto exclusivo para membros Max Seg" },
                                        contactPerson = contact.ifBlank { "Proprietário / Gerente" },
                                        phone = phone.ifBlank { "(15) 3552-0000" },
                                        address = address.ifBlank { "Centro - Apiaí/SP" },
                                        status = PartnerStatus.PROSPECCAO,
                                        argumentPitch = "Apresentar plano de segurança comercial e convênio de descontos para associados.",
                                        lat = -24.5094 + (Math.random() - 0.5) * 0.008,
                                        lng = -48.8427 + (Math.random() - 0.5) * 0.008,
                                        neighborhood = neighborhood.ifBlank { "Centro" }
                                    )
                                    // Optimistic update
                                    partnersList = listOf(newPartner) + partnersList

                                    coroutineScope.launch {
                                        val res = cloudRepo.addPartner(newPartner)
                                        isSaving = false
                                        showAddDialog = false
                                        if (res.isSuccess) {
                                            Toast.makeText(context, "Parceiro gravado na nuvem Firestore!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Cadastrado localmente", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                            enabled = !isSaving && name.isNotBlank()
                        ) {
                            if (isSaving) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            } else {
                                Text("Salvar no Firestore")
                            }
                        }
                    }
                }
            }
        }
    }

    // Sticker Technical Specs Dialog
    if (showStickerSpecsDialog) {
        Dialog(onDismissRequest = { showStickerSpecsDialog = false }) {
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
                    Text(
                        text = "Adesivo 'Estabelecimento Parceiro'",
                        color = MaxGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )

                    Text("• Aplicação: Porta de entrada de vidro / vitrine de cada comércio de Apiaí", color = Color.White, fontSize = 12.sp)
                    Text("• Material: Vinil Eletrostático ou Adesivo Polimérico Transparente com corte especial", color = Color.White, fontSize = 12.sp)
                    Text("• Tamanho: 15cm x 15cm circular ou escudo", color = Color.White, fontSize = 12.sp)
                    Text("• Acabamento: Verniz com Proteção UV Solar (não desbota com sol da tarde)", color = Color.White, fontSize = 12.sp)
                    Text("• Mensagem: 'ESTABELECIMENTO PARCEIRO - Aqui membro do Clube Max Seg Apiahy tem desconto exclusivo'", color = MaxGoldLight, fontSize = 12.sp)

                    Button(
                        onClick = { showStickerSpecsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Fechar")
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerDetailedCard(
    partner: PartnerBusiness,
    onClick: () -> Unit,
    onOpenOnMap: () -> Unit,
    onCall: () -> Unit,
    onWhatsApp: () -> Unit,
    onGoogleMaps: () -> Unit,
    onToggleStatus: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("partner_card_${partner.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header: Name, Segment, Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = partner.businessName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${partner.segment} • ${partner.neighborhood}",
                        color = MaxTextMuted,
                        fontSize = 11.sp
                    )
                }

                Surface(
                    onClick = onToggleStatus,
                    color = Color(partner.status.colorHex).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(Color(partner.status.colorHex), Color.White))
                    )
                ) {
                    Text(
                        text = partner.status.label,
                        color = Color(partner.status.colorHex),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Prominent Discount Information
            Surface(
                color = MaxDarkCardElevated,
                shape = RoundedCornerShape(8.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(MaxBurgundyLight, MaxGold))
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(MaxGold.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Percent,
                            contentDescription = null,
                            tint = MaxGold,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DESCONTO / BENEFÍCIO CLUBE MAX",
                            color = MaxGold,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = partner.benefitDiscount,
                            color = MaxGreenSuccess,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Contact Details Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaxTextMuted, modifier = Modifier.size(13.dp))
                    Text(
                        text = "Responsável: ${partner.contactPerson}",
                        color = MaxTextSecondary,
                        fontSize = 11.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Place, contentDescription = null, tint = MaxTextMuted, modifier = Modifier.size(13.dp))
                    Text(
                        text = "${partner.address} (${partner.neighborhood})",
                        color = MaxTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaxGold, modifier = Modifier.size(13.dp))
                    Text(
                        text = partner.phone,
                        color = MaxGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(color = MaxDarkCardElevated)

            // Direct Action Buttons: Ligar, WhatsApp, Google Maps, Ver no Mapa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Call Button
                FilledTonalButton(
                    onClick = onCall,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaxDarkCardElevated,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("call_partner_${partner.id}")
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = MaxGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Ligar", fontSize = 11.sp)
                }

                // WhatsApp Button
                FilledTonalButton(
                    onClick = onWhatsApp,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF1E7E34),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("whatsapp_partner_${partner.id}")
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WhatsApp", fontSize = 11.sp)
                }

                // Google Maps Navigation Button
                FilledTonalButton(
                    onClick = onGoogleMaps,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaxBurgundy,
                        contentColor = MaxGold
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier
                        .weight(1.1f)
                        .testTag("maps_partner_${partner.id}")
                ) {
                    Icon(Icons.Default.Navigation, contentDescription = null, tint = MaxGold, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Maps", fontSize = 11.sp, color = Color.White)
                }

                // Mini In-App Map Pin Button
                IconButton(
                    onClick = onOpenOnMap,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = MaxDarkCardElevated),
                    modifier = Modifier
                        .size(34.dp)
                        .testTag("pin_partner_${partner.id}")
                ) {
                    Icon(Icons.Default.Map, contentDescription = "Ver no Mapa", tint = MaxGold, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

/**
 * Normaliza o texto removendo acentos e convertendo para minúsculas para busca insensível a acentuação.
 */
private fun normalizeText(text: String): String {
    return Normalizer.normalize(text, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .lowercase()
        .trim()
}

/**
 * Expansão semântica para pesquisas cotidianas de serviços em Apiaí.
 */
private fun matchesServiceSynonyms(token: String, partner: PartnerBusiness): Boolean {
    val seg = normalizeText(partner.segment)
    val name = normalizeText(partner.businessName)
    val disc = normalizeText(partner.benefitDiscount)

    return when (token) {
        "farmacia", "farmacias", "drogaria", "remedio", "remedios", "medicamento", "medicamentos" -> {
            seg.contains("farmacia") || name.contains("drogaria") || name.contains("farmacia") || disc.contains("generico")
        }
        "clinica", "clinicas", "saude", "medico", "medica", "medicos", "dentista", "odonto", "odontologia",
        "exame", "exames", "laboratorio", "fisio", "fisioterapia", "consulta", "consultas" -> {
            seg.contains("clinica") || seg.contains("saude") || seg.contains("laboratorio") ||
                    name.contains("clinica") || name.contains("laboratorio") || disc.contains("consulta") || disc.contains("exame")
        }
        "mercado", "mercados", "supermercado", "supermercados", "compra", "compras", "cashback", "alimento", "alimentos" -> {
            seg.contains("supermercado") || name.contains("mercado") || name.contains("comprebem")
        }
        "posto", "postos", "gasolina", "etanol", "combustivel", "combustiveis", "diesel", "abastecer" -> {
            seg.contains("posto") || name.contains("posto") || disc.contains("litro")
        }
        "roupa", "roupas", "moda", "vestuario", "estilo", "boutique", "calcado", "calcados" -> {
            seg.contains("roupa") || seg.contains("moda") || name.contains("boutique") || name.contains("estilo")
        }
        "eletro", "movel", "moveis", "tv", "celular", "celulares", "geladeira", "eletrodomestico", "eletrodomesticos" -> {
            seg.contains("eletro") || seg.contains("moveis") || name.contains("eletro") || name.contains("moveis")
        }
        "pet", "pets", "racao", "animal", "animais", "veterinario", "veterinaria", "tosa", "banho", "agro" -> {
            seg.contains("pet") || seg.contains("racao") || name.contains("agro") || name.contains("pet")
        }
        "oficina", "oficinas", "mecanico", "mecanica", "carro", "carros", "moto", "motos", "peca", "pecas", "autopecas" -> {
            seg.contains("autopecas") || seg.contains("oficina") || name.contains("mecanica") || name.contains("auto pecas")
        }
        else -> false
    }
}

/**
 * Opens Google Maps Navigation or search query with exact business and address in Apiaí - SP.
 */
private fun openGoogleMapsPartner(context: Context, partner: PartnerBusiness) {
    val query = "${partner.businessName}, ${partner.address}, Apiaí - SP"
    val gmmIntentUri = Uri.parse("geo:${partner.lat},${partner.lng}?q=${Uri.encode(query)}")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(mapIntent)
    } catch (_: Exception) {
        // Fallback to web browser Google Maps search with exact coordinates and query
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")
        val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            Toast.makeText(context, "Não foi possível abrir o Google Maps.", Toast.LENGTH_SHORT).show()
        }
    }
}

/**
 * Opens WhatsApp with prefilled message identifying the user as a Max Seg Club member
 * inquiring about the partner discount.
 */
private fun openWhatsAppPartner(context: Context, partner: PartnerBusiness) {
    val cleanNumber = partner.phone.replace(Regex("[^0-9]"), "")
    val formattedNumber = if (cleanNumber.startsWith("55")) cleanNumber else "55$cleanNumber"
    val message = "Olá! Sou associado do Clube Max Seg Apiaí.\n" +
            "Gostaria de usufruir do benefício de parceiro no estabelecimento *${partner.businessName}*:\n" +
            "🎁 *Benefício / Desconto:* ${partner.benefitDiscount}\n" +
            "📍 *Endereço:* ${partner.address} (${partner.neighborhood})"

    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedNumber&text=" + Uri.encode(message))
    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        // Fallback to phone dialer if WhatsApp is not installed
        LocationHelper.openDialer(context, partner.phone)
    }
}

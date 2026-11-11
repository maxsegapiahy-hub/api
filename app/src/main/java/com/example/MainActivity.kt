package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SpartanBadgeVector
import com.example.ui.screens.*
import com.example.ui.theme.*

enum class AppNavDestination(
    val title: String,
    val icon: ImageVector,
    val testTag: String
) {
    CARD("Cartão VIP", Icons.Default.CreditCard, "nav_card"),
    ACCOUNT("Conta Max", Icons.Default.AccountCircle, "nav_account"),
    PARTNERS("Parceiros & Mapa", Icons.Default.Storefront, "nav_partners"),
    CAREER("Carreira & Pins", Icons.Default.MilitaryTech, "nav_career"),
    PLANS("Planos & Scripts", Icons.Default.Shield, "nav_plans")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MaxSegMainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaxSegMainApp() {
    var currentDestination by remember { mutableStateOf(AppNavDestination.CARD) }

    BackHandler(enabled = currentDestination != AppNavDestination.CARD) {
        currentDestination = AppNavDestination.CARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaxDarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SpartanBadgeVector(
                            size = 32.dp,
                            metalColor = MaxGold,
                            baseColor = MaxBurgundyDark
                        )
                        Column {
                            Text(
                                text = "MAX SEG APIAHY",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Clube de Benefícios & Afiliados",
                                color = MaxGold,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    Surface(
                        onClick = { currentDestination = AppNavDestination.PARTNERS },
                        color = MaxBurgundyDark,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.padding(end = 4.dp).testTag("top_btn_apiai_map")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(MaxGreenSuccess, shape = RoundedCornerShape(4.dp))
                            )
                            Text(
                                text = "APIAÍ-SP",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    IconButton(
                        onClick = { currentDestination = AppNavDestination.ACCOUNT },
                        modifier = Modifier.padding(end = 8.dp).testTag("top_btn_account")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Conta Max",
                            tint = if (currentDestination == AppNavDestination.ACCOUNT) MaxGold else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaxDarkSurface,
                    titleContentColor = Color.White
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaxDarkSurface,
                tonalElevation = 8.dp
            ) {
                AppNavDestination.entries.forEach { dest ->
                    val isSelected = currentDestination == dest
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = dest },
                        icon = {
                            Icon(
                                imageVector = dest.icon,
                                contentDescription = dest.title,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = {
                            Text(
                                text = dest.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaxGold,
                            selectedTextColor = MaxGold,
                            indicatorColor = MaxBurgundyDark,
                            unselectedIconColor = MaxTextMuted,
                            unselectedTextColor = MaxTextMuted
                        ),
                        modifier = Modifier.testTag(dest.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppNavDestination.CARD -> CardScreen(
                    onNavigateToAccount = { currentDestination = AppNavDestination.ACCOUNT }
                )
                AppNavDestination.ACCOUNT -> AccountScreen()
                AppNavDestination.PARTNERS -> PartnersScreen(initialMode = PartnersViewMode.LIST)
                AppNavDestination.CAREER -> CareerScreen()
                AppNavDestination.PLANS -> PlansScreen()
            }
        }
    }
}

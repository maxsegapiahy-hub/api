package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Tela unificada do Mapa de Apiaí e Estabelecimentos Parceiros.
 * Redireciona para [PartnersScreen] em modo Mapa, eliminando duplicidade de código e mapa.
 */
@Composable
fun MapScreen(
    modifier: Modifier = Modifier
) {
    PartnersScreen(
        modifier = modifier,
        initialMode = PartnersViewMode.MAP
    )
}

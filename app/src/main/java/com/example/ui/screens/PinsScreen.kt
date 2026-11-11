package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Tela unificada de Bottons & Pins de Graduação.
 * Redireciona para [CareerScreen] na aba de Pins, eliminando telas duplicadas na aplicação.
 */
@Composable
fun PinsScreen(
    modifier: Modifier = Modifier
) {
    CareerScreen(
        modifier = modifier,
        initialTab = CareerTab.PINS
    )
}

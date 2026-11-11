package com.example.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Tela unificada de Scripts e Pitches Comerciais.
 * Redireciona para [PlansScreen] na aba de Scripts, eliminando telas duplicadas na aplicação.
 */
@Composable
fun PitchScreen(
    modifier: Modifier = Modifier
) {
    PlansScreen(
        modifier = modifier,
        initialTab = PlansTab.SCRIPTS
    )
}

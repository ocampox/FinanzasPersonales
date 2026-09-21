package com.finanzas.personales.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Wrapper para animar items de listas (LazyColumn)
 * 
 * Agrega una animación de fade-in y slide-in cuando el item aparece
 * 
 * Requirements: 8.5
 */
@Composable
fun <T> AnimatedListItem(
    item: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
            initialOffsetY = { it / 2 },
            animationSpec = tween(300)
        ),
        exit = fadeOut(animationSpec = tween(200)) + slideOutVertically(
            targetOffsetY = { -it / 2 },
            animationSpec = tween(200)
        ),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            content(item)
        }
    }
}


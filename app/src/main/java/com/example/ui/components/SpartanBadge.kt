package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun SpartanBadgeVector(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    metalColor: Color = MaxGold,
    baseColor: Color = MaxBurgundyDark,
    accentGleam: Boolean = true,
    rankText: String? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gleam")
    val gleamOffset by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 300f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gleamOffset"
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(12.dp, CircleShape, spotColor = metalColor.copy(alpha = 0.6f))
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        baseColor,
                        MaxDarkBackground
                    )
                )
            )
            .border(
                width = (size.value * 0.05f).dp,
                brush = Brush.sweepGradient(
                    listOf(
                        metalColor,
                        Color.White.copy(alpha = 0.9f),
                        metalColor,
                        metalColor.copy(alpha = 0.4f),
                        metalColor
                    )
                ),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        // Spartan Helmet & Swords stylized geometric drawing
        Canvas(modifier = Modifier.fillMaxSize(0.82f)) {
            val w = this.size.width
            val h = this.size.height

            // Crossed Swords
            val swordStroke = w * 0.04f
            val swordBrush = Brush.linearGradient(listOf(metalColor, Color.White, metalColor))

            // Sword 1 (top-left to bottom-right)
            drawLine(
                brush = swordBrush,
                start = Offset(w * 0.15f, h * 0.15f),
                end = Offset(w * 0.85f, h * 0.85f),
                strokeWidth = swordStroke
            )
            // Sword 2 (top-right to bottom-left)
            drawLine(
                brush = swordBrush,
                start = Offset(w * 0.85f, h * 0.15f),
                end = Offset(w * 0.15f, h * 0.85f),
                strokeWidth = swordStroke
            )

            // Spartan Helmet Crest (Plume)
            val crestPath = Path().apply {
                moveTo(w * 0.5f, h * 0.12f)
                cubicTo(w * 0.35f, h * 0.15f, w * 0.38f, h * 0.32f, w * 0.45f, h * 0.35f)
                lineTo(w * 0.55f, h * 0.35f)
                cubicTo(w * 0.62f, h * 0.32f, w * 0.65f, h * 0.15f, w * 0.5f, h * 0.12f)
                close()
            }
            drawPath(
                path = crestPath,
                brush = Brush.verticalGradient(listOf(MaxBurgundyLight, MaxBurgundyDark))
            )
            drawPath(
                path = crestPath,
                color = metalColor,
                style = Stroke(width = w * 0.02f)
            )

            // Spartan Helmet Body
            val helmetPath = Path().apply {
                moveTo(w * 0.35f, h * 0.35f)
                cubicTo(w * 0.32f, h * 0.55f, w * 0.35f, h * 0.72f, w * 0.42f, h * 0.82f)
                lineTo(w * 0.46f, h * 0.82f)
                lineTo(w * 0.46f, h * 0.62f)
                lineTo(w * 0.54f, h * 0.62f)
                lineTo(w * 0.54f, h * 0.82f)
                lineTo(w * 0.58f, h * 0.82f)
                cubicTo(w * 0.65f, h * 0.72f, w * 0.68f, h * 0.55f, w * 0.65f, h * 0.35f)
                close()
            }

            drawPath(
                path = helmetPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        metalColor,
                        metalColor.copy(alpha = 0.7f),
                        baseColor
                    )
                )
            )
            drawPath(
                path = helmetPath,
                color = Color.White.copy(alpha = 0.5f),
                style = Stroke(width = w * 0.025f)
            )

            // Eye T-slit visor (Black cutout)
            val visorPath = Path().apply {
                // Horizontal T bar
                moveTo(w * 0.38f, h * 0.48f)
                lineTo(w * 0.62f, h * 0.48f)
                lineTo(w * 0.62f, h * 0.53f)
                lineTo(w * 0.53f, h * 0.53f)
                // Vertical T bar (nose guard)
                lineTo(w * 0.53f, h * 0.75f)
                lineTo(w * 0.47f, h * 0.75f)
                lineTo(w * 0.47f, h * 0.53f)
                lineTo(w * 0.38f, h * 0.53f)
                close()
            }
            drawPath(
                path = visorPath,
                color = Color.Black
            )

            // Gleam light beam sweep
            if (accentGleam) {
                drawLine(
                    color = Color.White.copy(alpha = 0.35f),
                    start = Offset(gleamOffset, 0f),
                    end = Offset(gleamOffset + 60f, h),
                    strokeWidth = w * 0.06f
                )
            }
        }

        if (rankText != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = (size.value * 0.08f).dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.85f),
                        shape = CircleShape
                    )
                    .border(1.dp, metalColor.copy(alpha = 0.8f), CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = rankText,
                    color = metalColor,
                    fontSize = (size.value * 0.11f).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MaxSegDataRepository
import com.example.model.PartnerBusiness
import com.example.model.PartnerStatus
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Coordinates reference for Apiaí - SP
private const val APIAI_CENTER_LAT = -24.5094
private const val APIAI_CENTER_LNG = -48.8427
private const val LAT_SPAN = 0.0240
private const val LNG_SPAN = 0.0240

data class PatrolUnit(
    val id: String,
    val name: String,
    val type: String, // "RONDA" or "SOS_APH"
    val lat: Double,
    val lng: Double,
    val speedKmh: Int,
    val status: String
)

@Composable
fun ApiaiInteractiveMap(
    modifier: Modifier = Modifier,
    partners: List<PartnerBusiness> = MaxSegDataRepository.initialPartners,
    selectedPartner: PartnerBusiness? = null,
    onSelectPartner: (PartnerBusiness?) -> Unit = {},
    showSosDispatchMode: Boolean = false,
    userLocationLat: Double = -24.5098,
    userLocationLng: Double = -48.8435,
    onCallEmergency: () -> Unit = {}
) {
    val context = LocalContext.current
    val textMeasurer = rememberTextMeasurer()

    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }
    var activeFilter by remember { mutableStateOf("Todos") }
    var showPatrolUnits by remember { mutableStateOf(true) }
    var showCoverageRadius by remember { mutableStateOf(true) }

    val categories = listOf("Todos", "Farmácia", "Supermercado", "Loja de Eletrodomésticos", "Loja de Roupas / Moda", "Posto de Combustíveis", "Pet Shop / Casa de Ração", "Laboratório de Exames")

    // Filtered partners
    val displayedPartners = partners.filter {
        activeFilter == "Todos" || it.segment == activeFilter
    }

    // Animation for radar pulse & patrol strobe
    val infiniteTransition = rememberInfiniteTransition(label = "mapAnim")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 10f,
        targetValue = 45f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarAlpha"
    )
    val patrolStrobe by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strobe"
    )

    // Moving patrol units in Apiaí
    val patrolTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "patrolMovement"
    )

    val patrolUnits = remember(patrolTime) {
        val angle1 = patrolTime * 2 * Math.PI.toFloat()
        val angle2 = (patrolTime + 0.5f) * 2 * Math.PI.toFloat()
        listOf(
            PatrolUnit(
                id = "V-01",
                name = "Viatura Max Ronda V-01",
                type = "RONDA",
                lat = APIAI_CENTER_LAT + 0.0035 * sin(angle1.toDouble()),
                lng = APIAI_CENTER_LNG + 0.0040 * cos(angle1.toDouble()),
                speedKmh = 32,
                status = "Patrulhamento Ostensivo Noturno"
            ),
            PatrolUnit(
                id = "V-02",
                name = "Unidade APH Socorro SOS V-02",
                type = "SOS_APH",
                lat = if (showSosDispatchMode) {
                    // Moving towards user during SOS dispatch
                    val progress = (patrolTime % 0.5f) / 0.5f
                    -24.5090 + (userLocationLat - (-24.5090)) * progress
                } else {
                    APIAI_CENTER_LAT - 0.0028 * cos(angle2.toDouble())
                },
                lng = if (showSosDispatchMode) {
                    val progress = (patrolTime % 0.5f) / 0.5f
                    -48.8420 + (userLocationLng - (-48.8420)) * progress
                } else {
                    APIAI_CENTER_LNG + 0.0032 * sin(angle2.toDouble())
                },
                speedKmh = if (showSosDispatchMode) 64 else 28,
                status = if (showSosDispatchMode) "DESLOCAMENTO DE EMERGÊNCIA APH" else "Prontidão Base Apiaí"
            )
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0D0D12))
    ) {
        // Main Map Canvas with touch handling for Drag & Tap
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panOffsetX += dragAmount.x
                        panOffsetY += dragAmount.y
                    }
                }
                .pointerInput(displayedPartners, zoomLevel, panOffsetX, panOffsetY) {
                    detectTapGestures { tapOffset ->
                        val w = size.width.toFloat()
                        val h = size.height.toFloat()
                        val cx = w / 2f + panOffsetX
                        val cy = h / 2f + panOffsetY
                        val scale = (w / LNG_SPAN.toFloat()) * zoomLevel

                        var clickedPartner: PartnerBusiness? = null
                        for (partner in displayedPartners) {
                            val px = cx + (partner.lng - APIAI_CENTER_LNG).toFloat() * scale
                            val py = cy - (partner.lat - APIAI_CENTER_LAT).toFloat() * scale
                            val dist = sqrt((tapOffset.x - px) * (tapOffset.x - px) + (tapOffset.y - py) * (tapOffset.y - py))
                            if (dist < 36.dp.toPx()) {
                                clickedPartner = partner
                                break
                            }
                        }
                        onSelectPartner(clickedPartner)
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerX = width / 2f + panOffsetX
            val centerY = height / 2f + panOffsetY
            val scale = (width / LNG_SPAN.toFloat()) * zoomLevel

            fun toScreenX(lng: Double): Float = centerX + (lng - APIAI_CENTER_LNG).toFloat() * scale
            fun toScreenY(lat: Double): Float = centerY - (lat - APIAI_CENTER_LAT).toFloat() * scale

            // 1. Draw Map Background & Grid
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF14141E), Color(0xFF0A0A0E)),
                    center = Offset(centerX, centerY),
                    radius = width * 1.2f
                )
            )

            // Coordinate Grid Lines
            val gridStep = 40.dp.toPx() * zoomLevel
            var gx = (centerX % gridStep)
            while (gx < width) {
                drawLine(
                    color = Color(0xFF1A1A28),
                    start = Offset(gx, 0f),
                    end = Offset(gx, height),
                    strokeWidth = 1f
                )
                gx += gridStep
            }
            var gy = (centerY % gridStep)
            while (gy < height) {
                drawLine(
                    color = Color(0xFF1A1A28),
                    start = Offset(0f, gy),
                    end = Offset(width, gy),
                    strokeWidth = 1f
                )
                gy += gridStep
            }

            // 2. Draw Natural Terrain & Parks (Alto Ribeira Park Area)
            val parkPath = Path().apply {
                moveTo(toScreenX(-48.8520), toScreenY(-24.5180))
                lineTo(toScreenX(-48.8460), toScreenY(-24.5240))
                lineTo(toScreenX(-48.8350), toScreenY(-24.5250))
                lineTo(toScreenX(-48.8310), toScreenY(-24.5190))
                lineTo(toScreenX(-48.8400), toScreenY(-24.5150))
                close()
            }
            drawPath(
                path = parkPath,
                color = Color(0xFF0F2418)
            )

            // 3. Draw Main Roads & Avenues of Apiaí - SP
            // Rodovia SP-250 (Main Highway connecting Apiaí)
            val sp250Path = Path().apply {
                moveTo(toScreenX(-48.8540), toScreenY(-24.5020))
                quadraticTo(
                    toScreenX(-48.8460), toScreenY(-24.5080),
                    toScreenX(-48.8427), toScreenY(-24.5094)
                )
                quadraticTo(
                    toScreenX(-48.8380), toScreenY(-24.5150),
                    toScreenX(-48.8320), toScreenY(-24.5260)
                )
            }
            // SP-250 Highway Outline & Core
            drawPath(path = sp250Path, color = Color(0xFF4A3818), style = Stroke(width = 10.dp.toPx() * zoomLevel, cap = StrokeCap.Round))
            drawPath(path = sp250Path, color = Color(0xFFFFB300), style = Stroke(width = 4.dp.toPx() * zoomLevel, cap = StrokeCap.Round))

            // Rua XV de Novembro (Centro Histórico)
            val rXvPath = Path().apply {
                moveTo(toScreenX(-48.8480), toScreenY(-24.5080))
                lineTo(toScreenX(-48.8380), toScreenY(-24.5095))
            }
            drawPath(path = rXvPath, color = Color(0xFF2E2E3E), style = Stroke(width = 7.dp.toPx() * zoomLevel, cap = StrokeCap.Round))
            drawPath(path = rXvPath, color = Color(0xFF8E8E9E), style = Stroke(width = 3.dp.toPx() * zoomLevel, cap = StrokeCap.Round))

            // Av. Leopoldo Leme
            val avLeopoldoPath = Path().apply {
                moveTo(toScreenX(-48.8450), toScreenY(-24.5050))
                lineTo(toScreenX(-48.8400), toScreenY(-24.5140))
            }
            drawPath(path = avLeopoldoPath, color = Color(0xFF2E2E3E), style = Stroke(width = 7.dp.toPx() * zoomLevel, cap = StrokeCap.Round))
            drawPath(path = avLeopoldoPath, color = Color(0xFF8E8E9E), style = Stroke(width = 3.dp.toPx() * zoomLevel, cap = StrokeCap.Round))

            // Rua do Comércio / Praça Alberto Dias Loop
            val rComercioPath = Path().apply {
                moveTo(toScreenX(-48.8460), toScreenY(-24.5070))
                lineTo(toScreenX(-48.8390), toScreenY(-24.5075))
                lineTo(toScreenX(-48.8410), toScreenY(-24.5120))
                lineTo(toScreenX(-48.8460), toScreenY(-24.5100))
                close()
            }
            drawPath(path = rComercioPath, color = Color(0xFF222232), style = Stroke(width = 5.dp.toPx() * zoomLevel, cap = StrokeCap.Round))

            // Av. das Nações & Rua José Loureiro
            val avNacoesPath = Path().apply {
                moveTo(toScreenX(-48.8490), toScreenY(-24.5120))
                lineTo(toScreenX(-48.8360), toScreenY(-24.5160))
            }
            drawPath(path = avNacoesPath, color = Color(0xFF222232), style = Stroke(width = 5.dp.toPx() * zoomLevel, cap = StrokeCap.Round))

            // 4. Neighborhood Labels
            val textStyle = TextStyle(
                color = Color(0xFF6B6B80),
                fontSize = (10 * zoomLevel).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            drawText(textMeasurer, "CENTRO HISTÓRICO", Offset(toScreenX(-48.8440), toScreenY(-24.5085)), textStyle)
            drawText(textMeasurer, "JARDIM PARAÍSO", Offset(toScreenX(-48.8480), toScreenY(-24.5135)), textStyle)
            drawText(textMeasurer, "PINHEIROS", Offset(toScreenX(-48.8470), toScreenY(-24.5055)), textStyle)
            drawText(textMeasurer, "VILA SANTA BÁRBARA", Offset(toScreenX(-48.8375), toScreenY(-24.5165)), textStyle)
            drawText(textMeasurer, "ALTO RIBEIRA / SP-250", Offset(toScreenX(-48.8340), toScreenY(-24.5230)), textStyle)

            // 5. Max Seg Protection Coverage Shield Ring (Cinturão de Proteção)
            if (showCoverageRadius) {
                val baseScreenX = toScreenX(-48.8420)
                val baseScreenY = toScreenY(-24.5090)
                val coverageRadiusPx = 160.dp.toPx() * zoomLevel

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(MaxBurgundyDark.copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(baseScreenX, baseScreenY),
                        radius = coverageRadiusPx
                    ),
                    radius = coverageRadiusPx,
                    center = Offset(baseScreenX, baseScreenY)
                )
                drawCircle(
                    color = MaxGold.copy(alpha = 0.35f),
                    radius = coverageRadiusPx,
                    center = Offset(baseScreenX, baseScreenY),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f))
                )
            }

            // 6. Base Central Operacional Max Seg
            val hqX = toScreenX(-48.8420)
            val hqY = toScreenY(-24.5090)
            drawCircle(color = MaxBurgundy, radius = 14.dp.toPx(), center = Offset(hqX, hqY))
            drawCircle(color = MaxGold, radius = 14.dp.toPx(), center = Offset(hqX, hqY), style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(hqX, hqY))
            drawText(
                textMeasurer = textMeasurer,
                text = "BASE MAX SEG 24H",
                topLeft = Offset(hqX - 45.dp.toPx(), hqY + 16.dp.toPx()),
                style = TextStyle(color = MaxGold, fontSize = 9.sp, fontWeight = FontWeight.Black)
            )

            // 7. Active Patrol Units (Ronda V-01 & APH SOS V-02)
            if (showPatrolUnits) {
                for (unit in patrolUnits) {
                    val ux = toScreenX(unit.lng)
                    val uy = toScreenY(unit.lat)

                    // Strobe light / warning halo
                    val unitColor = if (unit.type == "RONDA") Color(0xFF29B6F6) else Color(0xFFFF3D00)
                    drawCircle(
                        color = unitColor.copy(alpha = patrolStrobe * 0.4f),
                        radius = 18.dp.toPx(),
                        center = Offset(ux, uy)
                    )
                    drawCircle(
                        color = Color(0xFF0F0F1A),
                        radius = 10.dp.toPx(),
                        center = Offset(ux, uy)
                    )
                    drawCircle(
                        color = unitColor,
                        radius = 10.dp.toPx(),
                        center = Offset(ux, uy),
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                    drawCircle(
                        color = if (unit.type == "RONDA") Color(0xFF00E5FF) else Color(0xFFFFD600),
                        radius = 4.dp.toPx(),
                        center = Offset(ux, uy)
                    )

                    // Label
                    drawText(
                        textMeasurer = textMeasurer,
                        text = unit.id,
                        topLeft = Offset(ux - 12.dp.toPx(), uy - 20.dp.toPx()),
                        style = TextStyle(color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }

            // 8. SOS Emergency Dispatch Line & Route (if SOS Active)
            if (showSosDispatchMode) {
                val userX = toScreenX(userLocationLng)
                val userY = toScreenY(userLocationLat)
                val v2Unit = patrolUnits.find { it.id == "V-02" }
                if (v2Unit != null) {
                    val v2X = toScreenX(v2Unit.lng)
                    val v2Y = toScreenY(v2Unit.lat)

                    // Draw emergency route path
                    drawLine(
                        color = Color.Red,
                        start = Offset(v2X, v2Y),
                        end = Offset(userX, userY),
                        strokeWidth = 3.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 15f), patrolTime * 50f)
                    )
                }
            }

            // 9. Current User Location (GPS Ping)
            val userX = toScreenX(userLocationLng)
            val userY = toScreenY(userLocationLat)

            // Radar pulse wave
            drawCircle(
                color = if (showSosDispatchMode) Color.Red.copy(alpha = pulseAlpha) else MaxGold.copy(alpha = pulseAlpha),
                radius = pulseRadius * zoomLevel,
                center = Offset(userX, userY)
            )
            drawCircle(
                color = if (showSosDispatchMode) Color.Red else Color(0xFF2979FF),
                radius = 7.dp.toPx(),
                center = Offset(userX, userY)
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = Offset(userX, userY)
            )
            drawText(
                textMeasurer = textMeasurer,
                text = if (showSosDispatchMode) "ALERTA SOS ATIVO" else "SUA LOCALIZAÇÃO",
                topLeft = Offset(userX - 40.dp.toPx(), userY + 10.dp.toPx()),
                style = TextStyle(
                    color = if (showSosDispatchMode) Color.Red else Color(0xFF82B1FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            // 10. Partner Pins
            for (partner in displayedPartners) {
                val px = toScreenX(partner.lng)
                val py = toScreenY(partner.lat)
                val isSelected = selectedPartner?.id == partner.id

                val pinColor = when (partner.status) {
                    PartnerStatus.ATIVO -> MaxGreenSuccess
                    PartnerStatus.CONFIRMADO -> Color(0xFF29B6F6)
                    PartnerStatus.PROSPECCAO -> MaxGold
                }

                // Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.5f),
                    radius = if (isSelected) 18.dp.toPx() else 14.dp.toPx(),
                    center = Offset(px + 2f, py + 4f)
                )

                // Outer Ring
                drawCircle(
                    color = if (isSelected) Color.White else pinColor,
                    radius = if (isSelected) 16.dp.toPx() else 12.dp.toPx(),
                    center = Offset(px, py)
                )

                // Inner core
                drawCircle(
                    color = if (isSelected) MaxBurgundy else MaxDarkCard,
                    radius = if (isSelected) 13.dp.toPx() else 9.5.dp.toPx(),
                    center = Offset(px, py)
                )

                // Category Dot
                drawCircle(
                    color = pinColor,
                    radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                    center = Offset(px, py)
                )

                // Short Name Label
                val shortName = if (partner.businessName.length > 14) partner.businessName.take(12) + ".." else partner.businessName
                drawText(
                    textMeasurer = textMeasurer,
                    text = shortName,
                    topLeft = Offset(px - 35.dp.toPx(), py - 20.dp.toPx()),
                    style = TextStyle(
                        color = if (isSelected) MaxGold else Color.White,
                        fontSize = 8.5.sp,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold
                    )
                )
            }
        }

        // Top Control Overlay: Header & Category Pills
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Live Status Header Banner
            Surface(
                color = MaxDarkCard.copy(alpha = 0.92f),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxBurgundyLight, MaxGold)))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(if (showSosDispatchMode) Color.Red else MaxGreenSuccess, CircleShape)
                        )
                        Column {
                            Text(
                                text = if (showSosDispatchMode) "RADAR TÁTICO: SOCORRO EM DESLOCAMENTO" else "MAPA DE COBERTURA & PARCEIROS APIAHY",
                                color = if (showSosDispatchMode) Color.Red else MaxGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Apiaí - SP • 2 Viaturas Patrulhando • ${displayedPartners.size} Parceiros",
                                color = MaxTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Reset Center Button
                    IconButton(
                        onClick = {
                            panOffsetX = 0f
                            panOffsetY = 0f
                            zoomLevel = 1.0f
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.MyLocation, contentDescription = "Centralizar", tint = MaxGold, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Category Filter Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(categories) { cat ->
                    val isSelected = activeFilter == cat
                    Surface(
                        onClick = { activeFilter = cat },
                        color = if (isSelected) MaxBurgundy else MaxDarkCard.copy(alpha = 0.88f),
                        shape = RoundedCornerShape(20.dp),
                        border = if (isSelected) CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxGold, Color.White))) else null
                    ) {
                        Text(
                            text = cat,
                            color = if (isSelected) Color.White else MaxTextSecondary,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Floating Map Controls (Right Side: Zoom In, Zoom Out, Layer Toggle)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FloatingActionButton(
                onClick = { zoomLevel = (zoomLevel * 1.25f).coerceAtMost(3.0f) },
                containerColor = MaxDarkCard.copy(alpha = 0.9f),
                contentColor = MaxGold,
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Aumentar Zoom")
            }

            FloatingActionButton(
                onClick = { zoomLevel = (zoomLevel / 1.25f).coerceAtLeast(0.6f) },
                containerColor = MaxDarkCard.copy(alpha = 0.9f),
                contentColor = MaxGold,
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Diminuir Zoom")
            }

            FloatingActionButton(
                onClick = { showPatrolUnits = !showPatrolUnits },
                containerColor = if (showPatrolUnits) MaxBurgundy else MaxDarkCard.copy(alpha = 0.9f),
                contentColor = Color.White,
                modifier = Modifier.size(40.dp),
                shape = CircleShape
            ) {
                Icon(Icons.Default.LocalPolice, contentDescription = "Alternar Viaturas", modifier = Modifier.size(18.dp))
            }
        }

        // Bottom Selected Partner Detail Card Modal
        AnimatedVisibility(
            visible = selectedPartner != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(12.dp)
        ) {
            selectedPartner?.let { partner ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaxDarkCard),
                    shape = RoundedCornerShape(16.dp),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(MaxBurgundy, MaxGold))),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = partner.businessName,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${partner.segment} • ${partner.neighborhood}",
                                    color = MaxGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = { onSelectPartner(null) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar", tint = Color.White)
                            }
                        }

                        // Benefit Pill
                        Surface(
                            color = MaxDarkCardElevated,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Discount, contentDescription = null, tint = MaxGreenSuccess, modifier = Modifier.size(16.dp))
                                Text(
                                    text = partner.benefitDiscount,
                                    color = MaxGreenSuccess,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Text(
                            text = "📍 ${partner.address}",
                            color = MaxTextSecondary,
                            fontSize = 11.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val uri = Uri.parse("geo:${partner.lat},${partner.lng}?q=${Uri.encode(partner.businessName + " Apiaí")}")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Abrindo rota para ${partner.businessName}...", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.horizontalGradient(listOf(MaxGold, Color.White)))
                            ) {
                                Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rota GPS", fontSize = 11.sp)
                            }

                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${partner.phone.replace("[^0-9]".toRegex(), "")}"))
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Ligando para ${partner.phone}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaxBurgundy)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Ligar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

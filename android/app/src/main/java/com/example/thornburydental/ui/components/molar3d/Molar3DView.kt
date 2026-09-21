package com.example.thornburydental.ui.components.molar3d

import android.annotation.SuppressLint
import android.graphics.PixelFormat
import android.opengl.GLSurfaceView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.thornburydental.theme.*
import com.example.thornburydental.ui.components.FilamentToothView
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@SuppressLint("ClickableViewAccessibility")
@Composable
fun Molar3DView(
    modifier: Modifier = Modifier,
    showCard: Boolean = true
) {
    val isLocked by com.example.thornburydental.data.security.AppSessionLifecycleObserver.isLocked.collectAsState(initial = false)
    LegacyMolar3DView(
        modifier = modifier,
        showCard = showCard,
        isLocked = isLocked
    )
}

@SuppressLint("ClickableViewAccessibility")
@Composable
fun LegacyMolar3DView(
    modifier: Modifier = Modifier,
    showCard: Boolean = true,
    isLocked: Boolean = false
) {
    val context = LocalContext.current
    var model by remember { mutableStateOf<MolarModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedLayer by remember { mutableStateOf(MolarLayer.ALL) }
    var isAutoRotating by remember { mutableStateOf(true) }
    var isTouching by remember { mutableStateOf(false) }

    var rotX by remember { mutableFloatStateOf(12.0f) }
    var rotY by remember { mutableFloatStateOf(-30.0f) }

    val renderer = remember { MolarGLRenderer() }

    // Load OBJ model asynchronously
    LaunchedEffect(Unit) {
        try {
            val loadedModel = MolarObjParser.loadModel(context, "mandibular-first-molar.obj")
            model = loadedModel
            renderer.model = loadedModel
            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
        }
    }

    // Sync renderer properties
    LaunchedEffect(selectedLayer) {
        renderer.selectedLayer = selectedLayer
    }

    LaunchedEffect(rotX, rotY) {
        renderer.rotationX = rotX
        renderer.rotationY = rotY
    }

    // Gentle turntable auto-rotation when idle
    LaunchedEffect(isAutoRotating, isTouching) {
        if (isAutoRotating) {
            while (isActive) {
                if (!isTouching) {
                    rotY = (rotY + 0.35f) % 360f
                }
                delay(16)
            }
        }
    }

    if (!showCard) {
        Box(
            modifier = modifier
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { isTouching = true },
                        onDragEnd = { isTouching = false },
                        onDragCancel = { isTouching = false },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            rotY = (rotY + dragAmount.x * 0.45f) % 360f
                            rotX = (rotX + dragAmount.y * 0.45f).coerceIn(-75f, 75f)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = ThornburyPrimary,
                    strokeWidth = 3.dp
                )
            } else {
                AndroidView(
                    factory = { ctx ->
                        GLSurfaceView(ctx).apply {
                            setEGLContextClientVersion(2)
                            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                            holder.setFormat(PixelFormat.TRANSLUCENT)
                            setZOrderOnTop(true)
                            setRenderer(renderer)
                            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        return
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = ThornburySurfaceCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // --- Card Header ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ThornburyPrimary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Biotech,
                            contentDescription = null,
                            tint = ThornburyPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Mandibular First Molar",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "FDI 46 / Tooth #30 • 3D Anatomical Model",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }
                }

                // Control Action: Auto-Rotate Toggle & Reset
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = { isAutoRotating = !isAutoRotating },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = if (isAutoRotating) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                            contentDescription = if (isAutoRotating) "Pause Rotation" else "Auto-Rotate",
                            tint = if (isAutoRotating) ThornburyPrimary else ThornburyMuted,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            rotX = 12.0f
                            rotY = -30.0f
                            selectedLayer = MolarLayer.ALL
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = "Reset View",
                            tint = ThornburyMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- 3D Viewport with Touch Drag Rotation ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(ThornburySurfaceSoft)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isTouching = true },
                            onDragEnd = { isTouching = false },
                            onDragCancel = { isTouching = false },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                rotY = (rotY + dragAmount.x * 0.45f) % 360f
                                rotX = (rotX + dragAmount.y * 0.45f).coerceIn(-75f, 75f)
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = ThornburyPrimary,
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Loading 3D Molar Mesh...",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }
                } else {
                    AndroidView(
                        factory = { ctx ->
                            GLSurfaceView(ctx).apply {
                                setEGLContextClientVersion(2)
                                setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                                holder.setFormat(PixelFormat.TRANSLUCENT)
                                setZOrderOnTop(true)
                                setRenderer(renderer)
                                renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Touch Interaction Hint Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color.Black.copy(alpha = 0.55f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.TouchApp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Drag to rotate 360°",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White
                            )
                        }
                    }

                    // Polygon Count Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = ThornburyPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${model?.totalTriangles ?: 31288} Polygons",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.sp
                            ),
                            color = ThornburyPrimaryText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // --- Material 3 Layer Filter Chips ---
            Text(
                text = "Anatomical Layers",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = ThornburyInk
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MolarLayer.entries.forEach { layer ->
                    val isSelected = selectedLayer == layer
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedLayer = layer },
                        label = {
                            Text(
                                text = layer.displayName,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp
                                )
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ThornburyPrimary,
                            selectedLabelColor = Color.White,
                            containerColor = ThornburySurfaceSoft,
                            labelColor = ThornburyInk
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = ThornburyHairline,
                            selectedBorderColor = ThornburyPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Layer Clinical Note ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = ThornburySurfaceSoft,
                border = BorderStroke(1.dp, ThornburyHairline)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                when (selectedLayer) {
                                    MolarLayer.ALL -> ThornburyPrimary
                                    MolarLayer.ENAMEL -> Color(0xFFC0BCB5)
                                    MolarLayer.DENTIN -> Color(0xFFD4A84B)
                                    MolarLayer.ROOTS -> Color(0xFF9E5C3B)
                                },
                                CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedLayer.description,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = ThornburyBodyStrong
                    )
                }
            }
        }
    }
}

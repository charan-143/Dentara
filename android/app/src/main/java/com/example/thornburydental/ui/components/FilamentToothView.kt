package com.example.thornburydental.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.view.Choreographer
import android.view.Surface
import android.view.SurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.thornburydental.ui.components.molar3d.MolarLayer
import com.example.thornburydental.ui.components.molar3d.MolarModel
import com.example.thornburydental.ui.components.molar3d.MolarObjParser
import com.google.android.filament.*
import com.google.android.filament.android.UiHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.cos
import kotlin.math.sin

/**
 * Filament-powered 3D Anatomical Tooth Renderer Component.
 * Wraps Google Filament engine lifecycle (Engine, Renderer, Scene, Camera, View, SwapChain, LightManager, Skybox, IndirectLight)
 * inside a Compose AndroidView to render the anatomical 3D molar mesh with PBR lighting and touch arcball interaction.
 */
@SuppressLint("ClickableViewAccessibility")
@Composable
fun FilamentToothView(
    modifier: Modifier = Modifier,
    showCard: Boolean = true,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var model by remember { mutableStateOf<MolarModel?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedLayer by remember { mutableStateOf(MolarLayer.ALL) }
    var isAutoRotating by remember { mutableStateOf(true) }
    var isTouching by remember { mutableStateOf(false) }

    var rotX by remember { mutableFloatStateOf(12.0f) }
    var rotY by remember { mutableFloatStateOf(-30.0f) }

    // Load OBJ model asynchronously
    LaunchedEffect(Unit) {
        try {
            val loadedModel = MolarObjParser.loadModel(context, "mandibular-first-molar.obj")
            model = loadedModel
            isLoading = false
        } catch (e: Exception) {
            e.printStackTrace()
            isLoading = false
            onError?.invoke()
        }
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
                FilamentToothSurface(
                    model = model,
                    rotX = rotX,
                    rotY = rotY,
                    selectedLayer = selectedLayer,
                    onError = onError,
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
            // Header
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
                            text = "Mandibular First Molar (Filament PBR)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                            color = ThornburyInk
                        )
                        Text(
                            text = "FDI 46 / Tooth #30 • Filament PBR Engine",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }
                }

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

            // 3D Viewport
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
                            text = "Loading Filament PBR Engine & Mesh...",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThornburyMuted
                        )
                    }
                } else {
                    FilamentToothSurface(
                        model = model,
                        rotX = rotX,
                        rotY = rotY,
                        selectedLayer = selectedLayer,
                        onError = onError,
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
                                text = "Arcball Drag (Filament PBR)",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White
                            )
                        }
                    }

                    // Polygon Badge
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = ThornburyPrimary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${model?.totalTriangles ?: 31288} Polygons • Filament Engine",
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

            // Filter Chips
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
        }
    }
}

@Composable
private fun FilamentToothSurface(
    model: MolarModel?,
    rotX: Float,
    rotY: Float,
    selectedLayer: MolarLayer,
    onError: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    var controller by remember { mutableStateOf<FilamentToothController?>(null) }

    LaunchedEffect(rotX, rotY, selectedLayer) {
        controller?.apply {
            this.rotX = rotX
            this.rotY = rotY
            this.selectedLayer = selectedLayer
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            controller?.destroy()
            controller = null
        }
    }

    AndroidView(
        factory = { ctx ->
            SurfaceView(ctx).apply {
                holder.setFormat(PixelFormat.TRANSLUCENT)
                val ctrl = FilamentToothController(ctx, model, onError)
                controller = ctrl
                ctrl.attach(this)
            }
        },
        update = {
            controller?.apply {
                this.rotX = rotX
                this.rotY = rotY
                this.selectedLayer = selectedLayer
            }
        },
        modifier = modifier
    )
}

private class FilamentToothController(
    private val context: Context,
    private val model: MolarModel?,
    private val onError: (() -> Unit)?
) : UiHelper.RendererCallback {

    init {
        try {
            Filament.init()
        } catch (e: Throwable) {
            e.printStackTrace()
            onError?.invoke()
        }
    }

    private var engine: Engine? = null
    private var renderer: Renderer? = null
    private var scene: Scene? = null
    private var camera: Camera? = null
    private var view: View? = null
    private var swapChain: SwapChain? = null
    private var lightEntity: Int = 0
    private var skybox: Skybox? = null
    private var indirectLight: IndirectLight? = null
    private val uiHelper: UiHelper = UiHelper(UiHelper.ContextErrorPolicy.DONT_CHECK)
    private var isDestroyed = false

    var rotX: Float = 12.0f
    var rotY: Float = -30.0f
    var selectedLayer: MolarLayer = MolarLayer.ALL
    private val cameraDistance: Float = 3.8f

    private val createdEntities = mutableListOf<Int>()
    private val createdVertexBuffers = mutableListOf<VertexBuffer>()
    private val createdIndexBuffers = mutableListOf<IndexBuffer>()

    private val choreographer = Choreographer.getInstance()
    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (isDestroyed) return
            choreographer.postFrameCallback(this)
            renderFrame(frameTimeNanos)
        }
    }

    fun attach(surfaceView: SurfaceView) {
        try {
            uiHelper.renderCallback = this
            uiHelper.attachTo(surfaceView)
        } catch (t: Throwable) {
            t.printStackTrace()
            onError?.invoke()
        }
    }

    private fun setupFilamentEngine() {
        val eng = Engine.create()
        engine = eng
        renderer = eng.createRenderer()
        val scn = eng.createScene()
        scene = scn

        val camEntity = eng.entityManager.create()
        camera = eng.createCamera(camEntity)

        val v = eng.createView()
        v.scene = scn
        v.camera = camera
        view = v

        // Setup PBR Directional Lighting
        lightEntity = eng.entityManager.create()
        LightManager.Builder(LightManager.Type.DIRECTIONAL)
            .color(1.0f, 0.96f, 0.90f)
            .intensity(100000.0f)
            .direction(0.577f, -0.577f, -0.577f)
            .castShadows(true)
            .build(eng, lightEntity)
        scn.addEntity(lightEntity)

        // Skybox
        skybox = Skybox.Builder()
            .color(0.96f, 0.97f, 0.98f, 1.0f)
            .build(eng)
        scn.skybox = skybox

        // Populate mesh geometry into Filament scene
        if (model != null) {
            buildMeshEntities(eng, scn, model)
        }

        choreographer.postFrameCallback(frameCallback)
    }

    private fun buildMeshEntities(eng: Engine, scn: Scene, molarModel: MolarModel) {
        molarModel.subMeshes.forEach { subMesh ->
            try {
                val vb = VertexBuffer.Builder()
                    .vertexCount(subMesh.vertexCount)
                    .bufferCount(1)
                    .attribute(VertexBuffer.VertexAttribute.POSITION, 0, VertexBuffer.AttributeType.FLOAT3, 0, 24)
                    .attribute(VertexBuffer.VertexAttribute.TANGENTS, 0, VertexBuffer.AttributeType.FLOAT3, 12, 24)
                    .build(eng)

                subMesh.vertexBuffer.position(0)
                vb.setBufferAt(eng, 0, subMesh.vertexBuffer)
                createdVertexBuffers.add(vb)

                val ib = IndexBuffer.Builder()
                    .indexCount(subMesh.vertexCount)
                    .bufferType(IndexBuffer.Builder.IndexType.UINT)
                    .build(eng)

                val ibByteBuffer = ByteBuffer.allocateDirect(subMesh.vertexCount * 4).order(ByteOrder.nativeOrder())
                val ibIntBuffer = ibByteBuffer.asIntBuffer()
                for (i in 0 until subMesh.vertexCount) {
                    ibIntBuffer.put(i)
                }
                ibIntBuffer.position(0)
                ib.setBuffer(eng, ibIntBuffer)
                createdIndexBuffers.add(ib)

                val entity = eng.entityManager.create()
                RenderableManager.Builder(1)
                    .geometry(0, RenderableManager.PrimitiveType.TRIANGLES, vb, ib, 0, subMesh.vertexCount)
                    .culling(false)
                    .receiveShadows(true)
                    .castShadows(true)
                    .build(eng, entity)

                scn.addEntity(entity)
                createdEntities.add(entity)
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    override fun onNativeWindowChanged(surface: Surface) {
        try {
            if (engine == null) {
                setupFilamentEngine()
            }
            engine?.let { eng ->
                swapChain?.let { eng.destroySwapChain(it) }
                swapChain = eng.createSwapChain(surface)
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            onError?.invoke()
        }
    }

    override fun onDetachedFromSurface() {
        engine?.let { eng ->
            swapChain?.let { eng.destroySwapChain(it) }
            swapChain = null
        }
    }

    override fun onResized(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val aspect = width.toDouble() / height.toDouble()
        camera?.setProjection(45.0, aspect, 0.1, 100.0, Camera.Fov.VERTICAL)
        view?.viewport = Viewport(0, 0, width, height)
    }

    private fun updateCamera() {
        val radX = Math.toRadians(rotX.toDouble())
        val radY = Math.toRadians(rotY.toDouble())
        val eyeX = (cameraDistance * sin(radY) * cos(radX)).toFloat()
        val eyeY = (cameraDistance * sin(radX)).toFloat()
        val eyeZ = (cameraDistance * cos(radY) * cos(radX)).toFloat()

        camera?.lookAt(
            eyeX.toDouble(), eyeY.toDouble(), eyeZ.toDouble(),
            0.0, 0.0, 0.0,
            0.0, 1.0, 0.0
        )
    }

    private fun renderFrame(frameTimeNanos: Long) {
        if (isDestroyed) return
        updateCamera()
        val eng = engine ?: return
        val ren = renderer ?: return
        val v = view ?: return
        val sc = swapChain ?: return

        if (uiHelper.isReadyToRender) {
            if (ren.beginFrame(sc, frameTimeNanos)) {
                ren.render(v)
                ren.endFrame()
            }
        }
    }

    fun destroy() {
        isDestroyed = true
        choreographer.removeFrameCallback(frameCallback)
        uiHelper.detach()

        val eng = engine ?: return
        try {
            swapChain?.let { eng.destroySwapChain(it) }
            skybox?.let { eng.destroySkybox(it) }
            indirectLight?.let { eng.destroyIndirectLight(it) }

            createdEntities.forEach { entity ->
                scene?.removeEntity(entity)
                eng.destroyEntity(entity)
                eng.entityManager.destroy(entity)
            }
            createdEntities.clear()

            createdIndexBuffers.forEach { ib ->
                eng.destroyIndexBuffer(ib)
            }
            createdIndexBuffers.clear()

            createdVertexBuffers.forEach { vb ->
                eng.destroyVertexBuffer(vb)
            }
            createdVertexBuffers.clear()

            if (lightEntity != 0) {
                scene?.removeEntity(lightEntity)
                eng.destroyEntity(lightEntity)
                eng.entityManager.destroy(lightEntity)
            }

            camera?.let { eng.destroyCameraComponent(it.entity) }
            view?.let { eng.destroyView(it) }
            scene?.let { eng.destroyScene(it) }
            renderer?.let { eng.destroyRenderer(it) }
            eng.destroy()
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        engine = null
        swapChain = null
        renderer = null
        scene = null
        camera = null
        view = null
    }
}

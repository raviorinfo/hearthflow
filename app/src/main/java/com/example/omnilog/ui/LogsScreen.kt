package com.example.omnilog.ui

import android.content.Intent
import android.speech.RecognizerIntent
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.omnilog.data.model.LogCategory
import com.example.omnilog.data.model.LogEntry
import com.example.omnilog.ui.theme.*
import com.example.omnilog.viewmodel.MainViewModel
import kotlinx.coroutines.delay

// Sealed class representing display items in the feed
sealed class LogDisplayItem {
    data class Single(val log: LogEntry) : LogDisplayItem()
    data class BillGroup(
        val groupId: String,
        val groupName: String,
        val timestamp: Long,
        val childLogs: List<LogEntry>
    ) : LogDisplayItem()
}

@Composable
fun LogsScreen(viewModel: MainViewModel) {
    var inputText by remember { mutableStateOf("") }
    val allRawLogs by viewModel.allLogs.collectAsState()
    val logs = remember(allRawLogs) {
        allRawLogs.filter { !it.structuredData.contains("\"type\": \"DEBT_PAYMENT\"") }
    }
    val uiState by viewModel.uiState.collectAsState()
    var showScanDialog by remember { mutableStateOf(false) }

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
        if (spokenText != null) inputText = spokenText
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<LogCategory?>(null) }

    val filteredLogs = logs.filter { log ->
        val matchesSearch = log.content.contains(searchQuery, ignoreCase = true) || 
                          (log.groupName?.contains(searchQuery, ignoreCase = true) ?: false) ||
                          log.structuredData?.contains(searchQuery, ignoreCase = true) == true
        val matchesCategory = selectedCategory == null || log.category == selectedCategory
        matchesSearch && matchesCategory
    }

    // Perform dynamic grouping on the filtered logs
    val displayItems = remember(filteredLogs) {
        val groups = mutableMapOf<String, MutableList<LogEntry>>()
        val singles = mutableListOf<LogEntry>()
        
        filteredLogs.forEach { log ->
            val gId = log.groupId
            if (!gId.isNullOrBlank()) {
                groups.getOrPut(gId) { mutableListOf() }.add(log)
            } else {
                singles.add(log)
            }
        }
        
        val items = mutableListOf<LogDisplayItem>()
        
        // Add single logs
        singles.forEach { items.add(LogDisplayItem.Single(it)) }
        
        // Add grouped logs
        groups.forEach { (groupId, childLogs) ->
            val groupName = childLogs.firstOrNull { !it.groupName.isNullOrBlank() }?.groupName ?: "Scanned Bill"
            val timestamp = childLogs.maxOfOrNull { it.timestamp } ?: childLogs.firstOrNull()?.timestamp ?: System.currentTimeMillis()
            items.add(LogDisplayItem.BillGroup(groupId, groupName, timestamp, childLogs))
        }
        
        // Sort by timestamp descending
        items.sortedByDescending {
            when (it) {
                is LogDisplayItem.Single -> it.log.timestamp
                is LogDisplayItem.BillGroup -> it.timestamp
            }
        }
    }

    var searchFocused by remember { mutableStateOf(false) }
    val focusGlow by animateFloatAsState(
        targetValue = if (searchFocused) 0.6f else 0.15f,
        animationSpec = tween(400),
        label = "searchGlow"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            BrandGradientStart.copy(alpha = focusGlow),
            BrandGradientMid.copy(alpha = focusGlow * 0.6f),
            BrandGradientEnd.copy(alpha = focusGlow)
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Screen Header Card with Scan Bill option
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0x15FFFFFF))
                    .border(1.dp, Color(0x20FFFFFF), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GradientText(
                        text = "Activity Logs",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Scan receipts & track activities",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
                
                Button(
                    onClick = { showScanDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.pressScale()
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = "Scan Bill",
                        modifier = Modifier.size(18.dp),
                        tint = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Scan Bill", style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(16.dp))

            // Premium Frosted Search Box with focus glow shadow
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (searchFocused) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .blur(16.dp)
                            .background(
                                BrandGradientStart.copy(alpha = 0.2f),
                                RoundedCornerShape(18.dp)
                            )
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x16FFFFFF))
                        .border(1.5.dp, borderBrush, RoundedCornerShape(18.dp))
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search logs by item or store...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { searchFocused = it.isFocused },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Premium Custom Category Filter Row
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PremiumFilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = "All Archive"
                    )
                }
                LogCategory.values().forEach { category ->
                    item {
                        PremiumFilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = category.name.lowercase().replaceFirstChar { it.uppercase() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main Logs Feed
            LazyColumn(
                modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (displayItems.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x0AFFFFFF))
                                .border(1.dp, Color(0x10FFFFFF), RoundedCornerShape(20.dp))
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No history matches your search filter.", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        }
                    }
                } else {
                    items(displayItems) { displayItem ->
                        when (displayItem) {
                            is LogDisplayItem.Single -> LogItem(
                                log = displayItem.log,
                                onDeleteClick = { viewModel.deleteLog(displayItem.log.id) }
                            )
                            is LogDisplayItem.BillGroup -> BillGroupCard(
                                group = displayItem,
                                onDeleteGroup = { viewModel.deleteLogGroup(displayItem.groupId) }
                            )
                        }
                    }
                }
            }

            if (uiState is MainViewModel.UiState.Loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color(0x10FFFFFF)
                )
            }

            // Input OmniBar
            GlassOmniBar(
                inputText = inputText,
                onTextChange = { inputText = it },
                onVoiceClick = {
                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                    }
                    speechLauncher.launch(intent)
                },
                onSendClick = {
                    viewModel.processInput(inputText)
                    inputText = ""
                }
            )
        }

        // Render receipt scanner dialog
        if (showScanDialog) {
            ReceiptScannerDialog(
                onDismiss = { showScanDialog = false },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun BillGroupCard(
    group: LogDisplayItem.BillGroup,
    onDeleteGroup: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val totalAmount = remember(group.childLogs) {
        group.childLogs.sumOf { log ->
            val amountRegex = "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex()
            val priceRegex = "\"price\":\\s*(\\d+\\.?\\d*)".toRegex()
            val match = amountRegex.find(log.structuredData) ?: priceRegex.find(log.structuredData)
            match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
        }
    }

    val rotationState by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "arrowRotation"
    )

    val isDark = isSystemInDarkTheme()
    val bg = if (isDark) Color(0x1AFFFFFF) else Color(0xF2FFFFFF)

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            BrandViolet.copy(alpha = 0.4f),
            BrandCyan.copy(alpha = 0.2f),
            Color.Transparent
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, borderBrush, RoundedCornerShape(20.dp))
            .clickable { expanded = !expanded }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(listOf(BrandViolet.copy(alpha = 0.2f), BrandCyan.copy(alpha = 0.2f)))
                            )
                            .border(0.5.dp, BrandViolet.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = BrandCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = group.groupName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${group.childLogs.size} items",
                                style = MaterialTheme.typography.labelSmall,
                                color = BrandCyan
                            )
                            Spacer(Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = java.text.SimpleDateFormat("MMM dd, HH:mm", java.util.Locale.getDefault()).format(group.timestamp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formatCurrency(totalAmount),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = BrandViolet
                    )
                    IconButton(
                        onClick = onDeleteGroup,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Grouped Bill",
                            tint = Color(0xFFEF4444).copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Expand",
                            tint = Color.White,
                            modifier = Modifier.graphicsLayer { rotationZ = rotationState }
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(Modifier.height(12.dp))

                    group.childLogs.forEachIndexed { index, childLog ->
                        val isPantryItem = childLog.category == LogCategory.INVENTORY
                        val itemPrice = remember(childLog.structuredData) {
                            val amountRegex = "\"amount\":\\s*(\\d+\\.?\\d*)".toRegex()
                            val priceRegex = "\"price\":\\s*(\\d+\\.?\\d*)".toRegex()
                            val match = amountRegex.find(childLog.structuredData) ?: priceRegex.find(childLog.structuredData)
                            match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isPantryItem) BrandCyan else BrandViolet)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    val displayName = remember(childLog.content) {
                                        childLog.content
                                            .replace("Purchased ", "", ignoreCase = true)
                                            .replace(" for .*", "", ignoreCase = true)
                                            .split(" for ")[0]
                                    }
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = if (isPantryItem) "Pantry Stocked 🛒" else "General Expense 🧾",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isPantryItem) BrandCyan else BrandViolet
                                    )
                                }
                            }
                            Text(
                                text = formatCurrency(itemPrice),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isPantryItem) BrandCyan else BrandViolet
                            )
                        }

                        if (index < group.childLogs.lastIndex) {
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiptScannerDialog(
    onDismiss: () -> Unit,
    viewModel: MainViewModel
) {
    var storeName by remember { mutableStateOf("") }
    var receiptText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }
    var cameraMode by remember { mutableStateOf(false) }
    var isMockScanning by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            cameraMode = true
        }
    }

    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(2000) // Beautiful 2 seconds laser sweep scanning animation
            viewModel.scanBill(storeName, receiptText)
            isScanning = false
            onDismiss()
        }
    }

    if (cameraMode) {
        AlertDialog(
            onDismissRequest = { if (!isMockScanning) cameraMode = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { cameraMode = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Spacer(Modifier.width(8.dp))
                    GradientText(
                        text = "Align Receipt in Frame",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Position the store receipt inside the guidelines below. Tap Capture to run OCR text extraction.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                            .background(Color.Black)
                    ) {
                        if (hasCameraPermission) {
                            val lifecycleOwner = LocalLifecycleOwner.current
                            val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

                            AndroidView(
                                factory = { ctx ->
                                    val previewView = PreviewView(ctx).apply {
                                        scaleType = PreviewView.ScaleType.FILL_CENTER
                                    }
                                    val executor = ContextCompat.getMainExecutor(ctx)
                                    cameraProviderFuture.addListener({
                                        try {
                                            val cameraProvider = cameraProviderFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }
                                            val cameraSelector = when {
                                                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                                                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                                                else -> null
                                            }
                                            if (cameraSelector != null) {
                                                cameraProvider.unbindAll()
                                                cameraProvider.bindToLifecycle(
                                                    lifecycleOwner,
                                                    cameraSelector,
                                                    preview
                                                )
                                            } else {
                                                android.util.Log.e("CameraX", "No back or front camera available on this device")
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            android.util.Log.e("CameraX", "Failed to initialize CameraX: ${e.message}")
                                        }
                                    }, executor)
                                    previewView
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(Icons.Default.CameraEnhance, null, tint = BrandRose, modifier = Modifier.size(48.dp))
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        "Camera Permission Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandCyan)
                                    ) {
                                        Text("Grant Permission", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Guidelines overlay
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp)
                                .border(2.dp, BrandCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        ) {
                            // Subtle corner accents to make it look premium
                            Box(modifier = Modifier.align(Alignment.TopStart).size(20.dp).border(4.dp, BrandCyan, RoundedCornerShape(topStart = 8.dp)))
                            Box(modifier = Modifier.align(Alignment.TopEnd).size(20.dp).border(4.dp, BrandCyan, RoundedCornerShape(topEnd = 8.dp)))
                            Box(modifier = Modifier.align(Alignment.BottomStart).size(20.dp).border(4.dp, BrandCyan, RoundedCornerShape(bottomStart = 8.dp)))
                            Box(modifier = Modifier.align(Alignment.BottomEnd).size(20.dp).border(4.dp, BrandCyan, RoundedCornerShape(bottomEnd = 8.dp)))
                        }

                        // Scanning laser line animation
                        val infiniteTransition = rememberInfiniteTransition(label = "camlaser")
                        val laserY by infiniteTransition.animateFloat(
                            initialValue = 0.1f,
                            targetValue = 0.9f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1800, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "camlaserSweep"
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val y = size.height * laserY
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        BrandCyan,
                                        BrandCyan,
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(24.dp.toPx(), y),
                                end = Offset(size.width - 24.dp.toPx(), y),
                                strokeWidth = 3.dp.toPx()
                            )
                        }

                        // Mock Scanning Overlay
                        if (isMockScanning) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.8f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(color = BrandCyan)
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        "Extracting items via Smart OCR...",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        "Identifying matches & inventory structures...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isMockScanning = true },
                    enabled = !isMockScanning && hasCameraPermission,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                    modifier = Modifier.pressScale()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                        Text("Capture Receipt", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                if (!isMockScanning) {
                    TextButton(onClick = { cameraMode = false }) {
                        Text("Back to Form", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            containerColor = Color(0xFF131929),
            modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
        )

        // Handler for Mock Scanner
        LaunchedEffect(isMockScanning) {
            if (isMockScanning) {
                delay(2000) // Beautiful delay
                // Pick a random receipt template
                val templates = listOf(
                    Pair("Costco", "Milk $120\nOrganic Eggs $240\nWheat Bread $60\nFresh Apple $150\nChicken $950"),
                    Pair("Walmart", "Apples $180\nSoda $90\nCookies $120\nPaper Towels $200\nButter $250"),
                    Pair("Local Store", "Curry Powder $80\nBasmati Rice $120\nOnion $90\nPotato $60\nSugar $80"),
                    Pair("Starbucks", "Coffee Beans $450\nChocolate Cake $280\nVanilla Syrup $320\nMuffin $120")
                )
                val randomTemplate = templates.random()
                storeName = randomTemplate.first
                receiptText = randomTemplate.second
                isMockScanning = false
                cameraMode = false
            }
        }
    } else {
        AlertDialog(
            onDismissRequest = { if (!isScanning) onDismiss() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, null, tint = BrandViolet, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    GradientText(
                        text = "Smart Receipt Scanner",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight()) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Scan or paste your store receipt. Pantry matches will automatically restock your inventory, and all entries will be logically grouped under this receipt record.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        // Camera trigger button
                        Button(
                            onClick = {
                                if (hasCameraPermission) {
                                    cameraMode = true
                                } else {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandCyan),
                            modifier = Modifier.fillMaxWidth().pressScale(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Scan receipt using Camera 📸", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // Template Autofill
                        Text(
                            text = "Quick Mock Templates:",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = BrandCyan
                        )

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val templates = listOf(
                                Triple("Costco Wholesale 🛒", "Costco", "Milk $120\nOrganic Eggs $240\nWheat Bread $60\nFresh Apple $150\nChicken $950"),
                                Triple("Walmart Super 🛍️", "Walmart", "Apples $180\nSoda $90\nCookies $120\nPaper Towels $200\nButter $250"),
                                Triple("Local Pantry 🌶️", "Local Store", "Curry Powder $80\nBasmati Rice $120\nOnion $90\nPotato $60\nSugar $80"),
                                Triple("Starbucks Cafe ☕", "Starbucks", "Coffee Beans $450\nChocolate Cake $280\nVanilla Syrup $320\nMuffin $120")
                            )
                            items(templates) { (label, store, text) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0x1FFFFFFF))
                                        .border(0.5.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                        .clickable {
                                            storeName = store
                                            receiptText = text
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
                                }
                            }
                        }

                        // Store Name
                        OutlinedTextField(
                            value = storeName,
                            onValueChange = { storeName = it },
                            label = { Text("Store / Merchant Name") },
                            placeholder = { Text("e.g. Costco, Local Grocery") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedBorderColor = BrandViolet,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedLabelColor = BrandViolet,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                cursorColor = BrandViolet
                            )
                        )

                        // Receipt Content Box
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            OutlinedTextField(
                                value = receiptText,
                                onValueChange = { receiptText = it },
                                label = { Text("Receipt Items & Prices") },
                                placeholder = { Text("Milk $120\nEggs $240\nBread $60\n...") },
                                modifier = Modifier.fillMaxSize(),
                                maxLines = 8,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedBorderColor = BrandCyan,
                                    unfocusedBorderColor = Color(0x33FFFFFF),
                                    focusedLabelColor = BrandCyan,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    cursorColor = BrandCyan
                                )
                            )

                            // Laser sweep scanner overlay animation
                            if (isScanning) {
                                val infiniteTransition = rememberInfiniteTransition(label = "laser")
                                val laserY by infiniteTransition.animateFloat(
                                    initialValue = 0.05f,
                                    targetValue = 0.95f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = EaseInOutSine),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "laserSweep"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0x3A000000))
                                        .border(1.5.dp, BrandEmerald, RoundedCornerShape(4.dp))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val y = size.height * laserY
                                        drawLine(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    BrandEmerald,
                                                    BrandEmerald,
                                                    Color.Transparent
                                                )
                                            ),
                                            start = Offset(0f, y),
                                            end = Offset(size.width, y),
                                            strokeWidth = 4.dp.toPx()
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.Center)
                                            .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 14.dp, vertical = 8.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                color = BrandEmerald,
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                "Scanning receipt...",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = BrandEmerald
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { isScanning = true },
                    enabled = storeName.isNotBlank() && receiptText.isNotBlank() && !isScanning,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandViolet),
                    modifier = Modifier.pressScale()
                ) {
                    Text("Process Receipt", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isScanning) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            containerColor = Color(0xFF131929),
            modifier = Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(28.dp))
        )
    }
}

package com.example.programlauncher.adapter.input.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.example.programlauncher.LauncherApplication
import com.example.programlauncher.domain.model.GridPosition
import com.example.programlauncher.domain.model.LauncherItem
import com.example.programlauncher.domain.model.LauncherLayout
import com.example.programlauncher.domain.port.output.AppDetail
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels {
        LauncherViewModel.Factory((application as LauncherApplication).dependencyContainer)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFFBB86FC),
                    secondary = Color(0xFF03DAC5),
                    background = Color.Transparent, // Allows wallpaper to show through
                    surface = Color(0x99121212)    // Translucent glassmorphism
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    LauncherScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherScreen(viewModel: LauncherViewModel) {
    val layoutState by viewModel.layoutState.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val dbQueryTimeMs by viewModel.dbQueryTimeMs.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showDrawer by remember { mutableStateOf(false) }
    var selectedCellForAdd by remember { mutableStateOf<GridPosition?>(null) }
    var itemToRemove by remember { mutableStateOf<LauncherItem?>(null) }
    var activeFolder by remember { mutableStateOf<LauncherItem.Folder?>(null) }

    // Automatically monitor packages installed, uninstalled, or updated
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                viewModel.loadInstalledApps()
                viewModel.loadLayout()
                
                if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
                    val packageName = intent.data?.schemeSpecificPart
                    val isReplacing = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    if (packageName != null && !isReplacing) {
                        viewModel.removeShortcut(packageName)
                    }
                }
            }
        }
        val filter = android.content.IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, android.content.Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
    ) {
        // Main Screen Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with clock / status
            GlassmorphicHeader(
                dbQueryTimeMs = dbQueryTimeMs,
                onOpenDrawer = { showDrawer = true },
                onClearLayout = { viewModel.clearLayout() }
            )

            // Dynamic clock widget
            ClockWidget()

            Spacer(modifier = Modifier.height(16.dp))

            // Launcher grid view
            layoutState?.let { layout ->
                HomeScreenGrid(
                    layout = layout,
                    onShortcutClick = { shortcut ->
                        launchApp(context, shortcut.packageName, shortcut.className)
                    },
                    onShortcutLongClick = { shortcut ->
                        itemToRemove = shortcut
                    },
                    onFolderClick = { folder ->
                        activeFolder = folder
                    },
                    onFolderLongClick = { folder ->
                        itemToRemove = folder
                    },
                    onCellClick = { position ->
                        selectedCellForAdd = position
                    }
                )
            } ?: Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // App Drawer Panel
        if (showDrawer) {
            ModalBottomSheet(
                onDismissRequest = { showDrawer = false },
                containerColor = Color(0xEE1A1A1A),
                scrimColor = Color(0x66000000)
            ) {
                AppDrawerContent(
                    installedApps = installedApps,
                    onAppClick = { app ->
                        launchApp(context, app.packageName, app.className)
                        showDrawer = false
                    },
                    onAppLongClick = { app ->
                        // Offer to add to the first available spot
                        layoutState?.let { layout ->
                            var placed = false
                            for (y in 0 until layout.gridSize.rows) {
                                for (x in 0 until layout.gridSize.columns) {
                                    val pos = GridPosition(x, y, 0)
                                    if (layout.canPlaceAt(pos)) {
                                        viewModel.addShortcut(app.packageName, x, y, 0)
                                        placed = true
                                        Toast.makeText(context, "Added ${app.label} to ($x, $y)", Toast.LENGTH_SHORT).show()
                                        break
                                    }
                                }
                                if (placed) break
                            }
                            if (!placed) {
                                Toast.makeText(context, "No empty grid spaces found!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        showDrawer = false
                    }
                )
            }
        }

        // Add Shortcut Dialog
        selectedCellForAdd?.let { pos ->
            AddShortcutDialog(
                position = pos,
                installedApps = installedApps,
                onDismiss = { selectedCellForAdd = null },
                onAppSelected = { app ->
                    viewModel.addShortcut(app.packageName, pos.x, pos.y, pos.screen)
                    selectedCellForAdd = null
                }
            )
        }

        // Confirm Remove Dialog
        itemToRemove?.let { item ->
            val title = if (item is LauncherItem.Folder) "Remove Folder?" else "Remove Shortcut?"
            val message = if (item is LauncherItem.Folder) "Are you sure you want to remove '${item.label}' and all its contents?" else "Are you sure you want to remove the shortcut for ${item.label}?"
            AlertDialog(
                onDismissRequest = { itemToRemove = null },
                title = { Text(title) },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.removeShortcutAt(item.position.x, item.position.y, item.position.screen)
                        itemToRemove = null
                    }) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { itemToRemove = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Opened Folder Dialog
        activeFolder?.let { folder ->
            val currentFolder = layoutState?.items?.find { it.id == folder.id } as? LauncherItem.Folder ?: folder
            
            Dialog(onDismissRequest = { activeFolder = null }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xEE1A1A1A)
                    ),
                    border = BorderStroke(1.dp, Color(0x22FFFFFF))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Text(
                            text = currentFolder.label,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = (-0.5).sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        if (currentFolder.items.isNotEmpty()) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(4),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(currentFolder.items) { app ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                launchApp(context, app.packageName, app.className)
                                                activeFolder = null
                                            }
                                            .padding(8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        AppIcon(packageName = app.packageName, modifier = Modifier.size(48.dp))
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = app.label,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Empty folder.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { activeFolder = null },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClockWidget() {
    var timeText by remember { mutableStateOf("--:--") }
    var dateText by remember { mutableStateOf("---") }

    LaunchedEffect(Unit) {
        while (true) {
            val calendar = java.util.Calendar.getInstance()
            timeText = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(calendar.time)
            dateText = java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale.getDefault()).format(calendar.time)
            kotlinx.coroutines.delay(1000)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Text(
            text = timeText,
            fontSize = 64.sp,
            fontWeight = FontWeight.Light,
            color = Color.White,
            letterSpacing = (-2).sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xB3FFFFFF),
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GlassmorphicHeader(
    dbQueryTimeMs: Double,
    onOpenDrawer: () -> Unit,
    onClearLayout: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0x40222222)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AGY Launcher",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = (-0.5).sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "DDD & Hexagonal",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        letterSpacing = (-0.2).sp
                    )
                    // Profile/performance badge
                    Surface(
                        color = Color(0x2200FFCC),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "⚡ DB: %.2f ms", dbQueryTimeMs),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00FFCC),
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onOpenDrawer,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x60BB86FC)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("All Apps", fontSize = 12.sp)
                }

                Button(
                    onClick = onClearLayout,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x40CF6679)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset Grid", fontSize = 12.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderItemView(
    folder: LauncherItem.Folder,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 2x2 preview card
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0x22FFFFFF), RoundedCornerShape(14.dp))
                .border(1.dp, Color(0x11FFFFFF), RoundedCornerShape(14.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            val previewItems = folder.items.take(4)
            if (previewItems.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(previewItems) { app ->
                        AppIcon(
                            packageName = app.packageName,
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(4.dp))
                        )
                    }
                }
            } else {
                Text(
                    text = "empty",
                    fontSize = 8.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = folder.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ColumnScope.HomeScreenGrid(
    layout: LauncherLayout,
    onShortcutClick: (LauncherItem.AppShortcut) -> Unit,
    onShortcutLongClick: (LauncherItem.AppShortcut) -> Unit,
    onFolderClick: (LauncherItem.Folder) -> Unit,
    onFolderLongClick: (LauncherItem.Folder) -> Unit,
    onCellClick: (GridPosition) -> Unit
) {
    val itemsByPos = remember(layout.items) {
        layout.items.associateBy { it.position }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(24.dp))
            .background(Color(0x22111111), RoundedCornerShape(24.dp))
            .padding(12.dp)
    ) {
        for (y in 0 until layout.gridSize.rows) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (x in 0 until layout.gridSize.columns) {
                    val pos = GridPosition(x, y, 0)
                    val item = itemsByPos[pos]

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item != null) {
                            when (item) {
                                is LauncherItem.AppShortcut -> {
                                    GridItemView(
                                        shortcut = item,
                                        onClick = { onShortcutClick(item) },
                                        onLongClick = { onShortcutLongClick(item) }
                                    )
                                }
                                is LauncherItem.Folder -> {
                                    FolderItemView(
                                        folder = item,
                                        onClick = { onFolderClick(item) },
                                        onLongClick = { onFolderLongClick(item) }
                                    )
                                }
                            }
                        } else {
                            EmptyCellView(
                                onClick = { onCellClick(pos) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GridItemView(
    shortcut: LauncherItem.AppShortcut,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIcon(
            packageName = shortcut.packageName,
            modifier = Modifier
                .size(48.dp)
                .padding(2.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = shortcut.label,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyCellView(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x0AFFFFFF), RoundedCornerShape(16.dp))
            .clickable(
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            color = Color(0x33FFFFFF),
            fontSize = 18.sp,
            fontWeight = FontWeight.Light
        )
    }
}

// Simple thread-safe memory cache for loaded icons to avoid repeating heavy PackageManager queries
private val iconCache = java.util.concurrent.ConcurrentHashMap<String, Drawable>()

@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var iconDrawable by remember(packageName) { mutableStateOf<Drawable?>(iconCache[packageName]) }

    if (iconDrawable == null) {
        LaunchedEffect(packageName) {
            val drawable = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                getAppIcon(context, packageName)
            }
            iconDrawable = drawable
        }
    }

    if (iconDrawable != null) {
        AndroidView(
            factory = { ctx ->
                ImageView(ctx).apply {
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageDrawable(iconDrawable)
                }
            },
            update = { imageView ->
                if (imageView.drawable != iconDrawable) {
                    imageView.setImageDrawable(iconDrawable)
                }
            },
            modifier = modifier
        )
    } else {
        // Glassmorphic placeholder while loading asynchronously
        Box(
            modifier = modifier
                .background(Color(0x15FFFFFF), RoundedCornerShape(12.dp))
        )
    }
}

@Composable
fun AppDrawerContent(
    installedApps: List<AppDetail>,
    onAppClick: (AppDetail) -> Unit,
    onAppLongClick: (AppDetail) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.label.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.8f)
            .padding(16.dp)
    ) {
        Text(
            text = "All Applications",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Hold down an app to pin it to the home screen.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Glassmorphic search bar
        TextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search apps...", color = Color.Gray, fontSize = 14.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0x22FFFFFF),
                unfocusedContainerColor = Color(0x15FFFFFF),
                cursorColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(14.dp),
            leadingIcon = {
                Text("🔍", fontSize = 16.sp, modifier = Modifier.padding(start = 8.dp))
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Text("✕", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
        )

        if (filteredApps.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No apps found for \"$searchQuery\"",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredApps) { app ->
                    AppDrawerItem(
                        app = app,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerItem(
    app: AppDetail,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AppIcon(
            packageName = app.packageName,
            modifier = Modifier.size(54.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = app.label,
            color = Color.White,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AddShortcutDialog(
    position: GridPosition,
    installedApps: List<AppDetail>,
    onDismiss: () -> Unit,
    onAppSelected: (AppDetail) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Place app at (${position.x}, ${position.y})",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(installedApps) { app ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onAppSelected(app) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AppIcon(packageName = app.packageName, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = app.label,
                                color = Color.White,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

private fun launchApp(context: Context, packageName: String, className: String) {
    try {
        val intent = Intent().apply {
            setClassName(packageName, className)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "Could not launch app: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun getAppIcon(context: Context, packageName: String): Drawable? {
    iconCache[packageName]?.let { return it }
    return try {
        val drawable = context.packageManager.getApplicationIcon(packageName)
        iconCache[packageName] = drawable
        drawable
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
}

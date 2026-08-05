package com.rynekryz.rynotes

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Close
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private sealed class HomeFilter {
    data object All : HomeFilter()
    data object Favorites : HomeFilter()
    data object Archive : HomeFilter()
    data class Folder(val id: String) : HomeFilter()
    data class Tag(val name: String) : HomeFilter()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: NoteViewModel,
    onNavigateToSettings: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToVault: () -> Unit,
    onOpenNote: (String) -> Unit,
    onOpenPdf: (android.net.Uri) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf<HomeFilter>(HomeFilter.All) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var showNewFolderDialog by remember { mutableStateOf(false) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) onOpenPdf(uri) }

    val baseNotes = when (val f = filter) {
        HomeFilter.All -> viewModel.sortedNotes()
        HomeFilter.Favorites -> viewModel.favoriteNotes()
        HomeFilter.Archive -> viewModel.archivedNotes()
        is HomeFilter.Folder -> viewModel.notesInFolder(f.id)
        is HomeFilter.Tag -> viewModel.notesWithTag(f.name)
    }

    val visibleNotes = if (searchQuery.isBlank()) {
        baseNotes
    } else {
        baseNotes.filter {
            it.text.contains(searchQuery, ignoreCase = true) || it.title.contains(searchQuery, ignoreCase = true)
        }
    }

    val screenTitle = when (val f = filter) {
        HomeFilter.All -> "RyNotes"
        HomeFilter.Favorites -> "Favorites"
        HomeFilter.Archive -> "Archive"
        is HomeFilter.Folder -> viewModel.folderById(f.id)?.name ?: "Folder"
        is HomeFilter.Tag -> "#${f.name}"
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            HomeDrawerContent(
                viewModel = viewModel,
                selectedFilter = filter,
                onSelectAll = { filter = HomeFilter.All; scope.launch { drawerState.close() } },
                onSelectFavorites = { filter = HomeFilter.Favorites; scope.launch { drawerState.close() } },
                onSelectArchive = { filter = HomeFilter.Archive; scope.launch { drawerState.close() } },
                onSelectFolder = { id -> filter = HomeFilter.Folder(id); scope.launch { drawerState.close() } },
                onSelectTag = { tag -> filter = HomeFilter.Tag(tag); scope.launch { drawerState.close() } },
                onNewFolder = { showNewFolderDialog = true },
                onVaultClick = { scope.launch { drawerState.close() }; onNavigateToVault() },
                onTrashClick = { scope.launch { drawerState.close() }; onNavigateToTrash() },
                onOpenPdfClick = { scope.launch { drawerState.close() }; pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                onSettingsClick = { scope.launch { drawerState.close() }; onNavigateToSettings() }
            )
        }
    ) {
        Scaffold(
            topBar = {
                HomeTopBar(
                    title = screenTitle,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            },
            floatingActionButton = {
                val hapticsOn = LocalHapticsEnabled.current
                val haptics = rememberHaptics(hapticsOn)
                ExtendedFloatingActionButton(
                    onClick = {
                        haptics.confirm()
                        val folderId = (filter as? HomeFilter.Folder)?.id
                        onOpenNote(viewModel.createNote(folderId))
                    },
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New note") }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { padding ->
            when {
                viewModel.isLoading -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        LoadingIndicator()
                    }
                }
                baseNotes.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(padding)) { EmptyState(filter) }
                }
                visibleNotes.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(padding)) { NoSearchResults(searchQuery) }
                }
                else -> {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(12.dp),
                        verticalItemSpacing = 10.dp,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visibleNotes, key = { it.id }) { note ->
                            NoteGridCard(note = note, onClick = { onOpenNote(note.id) })
                        }
                        item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                            Spacer(Modifier.height(72.dp))
                        }
                    }
                }
            }
        }
    }

    if (showNewFolderDialog) {
        var name by remember { mutableStateOf("") }
        val dialogHaptics = rememberHaptics(LocalHapticsEnabled.current)
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Folder name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dialogHaptics.click()
                    if (name.isNotBlank()) {
                        val id = viewModel.createFolder(name)
                        filter = HomeFilter.Folder(id)
                    }
                    showNewFolderDialog = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { dialogHaptics.click(); showNewFolderDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun HomeDrawerContent(
    viewModel: NoteViewModel,
    selectedFilter: HomeFilter,
    onSelectAll: () -> Unit,
    onSelectFavorites: () -> Unit,
    onSelectArchive: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectTag: (String) -> Unit,
    onNewFolder: () -> Unit,
    onVaultClick: () -> Unit,
    onTrashClick: () -> Unit,
    onOpenPdfClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(12.dp))
            Text(
                "RyNotes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(8.dp))

            NavigationDrawerItem(
                label = { Text("All notes") },
                icon = { Icon(Icons.AutoMirrored.Outlined.StickyNote2, contentDescription = null) },
                selected = selectedFilter is HomeFilter.All,
                onClick = { haptics.click(); onSelectAll() },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            NavigationDrawerItem(
                label = { Text("Favorites") },
                icon = { Icon(Icons.Filled.Star, contentDescription = null) },
                selected = selectedFilter is HomeFilter.Favorites,
                onClick = { haptics.click(); onSelectFavorites() },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(8.dp))
            DrawerSectionHeader(text = "Folders", onAddClick = { haptics.click(); onNewFolder() })
            viewModel.folders.value.forEach { folder ->
                NavigationDrawerItem(
                    label = { Text(folder.name) },
                    icon = { Icon(Icons.Filled.Folder, contentDescription = null) },
                    selected = selectedFilter is HomeFilter.Folder && selectedFilter.id == folder.id,
                    onClick = { haptics.click(); onSelectFolder(folder.id) },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
            if (viewModel.folders.value.isEmpty()) {
                Text(
                    "No folders yet",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )
            }

            val tags = viewModel.allTags()
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                DrawerSectionHeader(text = "Tags")
                tags.forEach { tag ->
                    NavigationDrawerItem(
                        label = { Text(tag) },
                        icon = { Icon(Icons.Filled.Sell, contentDescription = null) },
                        selected = selectedFilter is HomeFilter.Tag && selectedFilter.name == tag,
                        onClick = { haptics.click(); onSelectTag(tag) },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(8.dp))

            NavigationDrawerItem(
                label = { Text("Archive") },
                icon = { Icon(Icons.Filled.Archive, contentDescription = null) },
                selected = selectedFilter is HomeFilter.Archive,
                onClick = { haptics.click(); onSelectArchive() },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            NavigationDrawerItem(
                label = { Text("Vault") },
                icon = { Icon(Icons.Filled.Shield, contentDescription = null) },
                selected = false,
                onClick = { haptics.click(); onVaultClick() },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            NavigationDrawerItem(
                label = { Text("Trash") },
                icon = { Icon(Icons.Filled.DeleteOutline, contentDescription = null) },
                selected = false,
                onClick = { haptics.click(); onTrashClick() },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            NavigationDrawerItem(
                label = { Text("Open PDF") },
                icon = { Icon(Icons.Filled.PictureAsPdf, contentDescription = null) },
                selected = false,
                onClick = { haptics.click(); onOpenPdfClick() },
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

            NavigationDrawerItem(
                label = { Text("Settings") },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                selected = false,
                onClick = { haptics.click(); onSettingsClick() },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerSectionHeader(text: String, onAddClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            fontWeight = FontWeight.Bold
        )
        if (onAddClick != null) {
            IconButton(onClick = onAddClick, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "New folder", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar(
    title: String,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onMenuClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        val haptics = rememberHaptics(LocalHapticsEnabled.current)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            IconButton(onClick = { haptics.click(); onMenuClick() }) {
                Icon(Icons.Filled.Menu, contentDescription = "Open menu")
            }
            Spacer(Modifier.width(4.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search notes") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
fun NoteGridCard(
    note: Note,
    onClick: () -> Unit
) {
    val hapticsOn = LocalHapticsEnabled.current
    val haptics = rememberHaptics(hapticsOn)
    val containerColor = noteCardColor(note.colorTag)
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.95f else 1f,
        animationSpec = if (pressed) {
            androidx.compose.animation.core.tween(durationMillis = 50)
        } else {
            androidx.compose.animation.core.spring(
                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                stiffness = androidx.compose.animation.core.Spring.StiffnessMedium
            )
        },
        label = "cardScale"
    )
    Surface(
        onClick = {
            haptics.click()
            onClick()
        },
        interactionSource = interactionSource,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        tonalElevation = if (note.pinned) 2.dp else 0.dp,
        modifier = Modifier.scale(scale)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    if (note.title.isNotBlank()) {
                        Text(
                            text = note.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    if (note.text.isNotBlank()) {
                        Text(
                            text = note.text,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = if (note.title.isNotBlank()) 6 else 8,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    if (note.pinned) {
                        Icon(
                            imageVector = Icons.Filled.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (note.favorite) {
                        Spacer(Modifier.height(4.dp))
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            if (note.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = note.tags.joinToString(" ") { "#$it" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = relativeTime(note.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun noteCardColor(colorTag: String): Color {
    val scheme = MaterialTheme.colorScheme
    return when (runCatching { NoteColor.valueOf(colorTag) }.getOrDefault(NoteColor.DEFAULT)) {
        NoteColor.DEFAULT -> scheme.surfaceVariant.copy(alpha = 0.45f)
        NoteColor.AMBER -> scheme.primaryContainer.copy(alpha = 0.55f)
        NoteColor.SAGE -> scheme.tertiaryContainer.copy(alpha = 0.55f)
        NoteColor.SKY -> Color(0xFFD3E4F5).copy(alpha = if (isSystemDark()) 0.25f else 0.7f)
        NoteColor.ROSE -> Color(0xFFF6D8DC).copy(alpha = if (isSystemDark()) 0.25f else 0.7f)
        NoteColor.LAVENDER -> Color(0xFFE3DAF6).copy(alpha = if (isSystemDark()) 0.25f else 0.7f)
    }
}

@Composable
private fun isSystemDark() = androidx.compose.foundation.isSystemInDarkTheme()

@Composable
private fun EmptyState(filter: HomeFilter) {
    val (title, subtitle) = when (filter) {
        HomeFilter.All -> "No notes yet" to "Tap \"New note\" to write your first one"
        HomeFilter.Favorites -> "No favorites yet" to "Star a note to find it here"
        HomeFilter.Archive -> "Archive is empty" to "Archived notes show up here"
        is HomeFilter.Folder -> "This folder is empty" to "Move or create a note here"
        is HomeFilter.Tag -> "No notes with this tag" to "Tag a note to find it here"
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.StickyNote2,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NoSearchResults(query: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No results for \"$query\"",
            style = MaterialTheme.typography.titleMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun relativeTime(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}m ago"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}h ago"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d ago"
        else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}

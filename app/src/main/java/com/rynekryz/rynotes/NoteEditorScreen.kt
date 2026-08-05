package com.rynekryz.rynotes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Sell
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteViewModel,
    noteId: String,
    onClose: () -> Unit,
) {
    val note = remember(noteId) { viewModel.noteById(noteId) }
    var title by rememberSaveable(noteId) { mutableStateOf(note?.title.orEmpty()) }
    var bodyField by remember(noteId) { mutableStateOf(TextFieldValue(note?.text.orEmpty())) }
    var pinned by rememberSaveable(noteId) { mutableStateOf(note?.pinned ?: false) }
    var favorite by rememberSaveable(noteId) { mutableStateOf(note?.favorite ?: false) }
    var colorTag by rememberSaveable(noteId) { mutableStateOf(note?.colorTag ?: NoteColor.DEFAULT.name) }
    var folderId by rememberSaveable(noteId) { mutableStateOf(note?.folderId) }
    var tags by remember(noteId) { mutableStateOf(note?.tags ?: emptyList()) }
    var locked by rememberSaveable(noteId) { mutableStateOf(note?.locked ?: false) }
    var archived by rememberSaveable(noteId) { mutableStateOf(note?.archivedAt != null) }
    var showColorPicker by rememberSaveable(noteId) { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable(noteId) { mutableStateOf(false) }
    var showFolderPicker by rememberSaveable(noteId) { mutableStateOf(false) }
    var showTagEditor by rememberSaveable(noteId) { mutableStateOf(false) }
    var previewMode by rememberSaveable(noteId) { mutableStateOf(false) }

    val titleFocusRequester = remember { FocusRequester() }
    val haptics = rememberHaptics(viewModel.hapticsEnabled)

    LaunchedEffect(noteId) {
        if (title.isEmpty() && bodyField.text.isEmpty()) {
            delay(150.milliseconds)
            titleFocusRequester.requestFocus()
        }
    }

    fun handleSave() {
        if (title.trim().isEmpty() && bodyField.text.trim().isEmpty()) {
            viewModel.trashNote(noteId)
        } else {
            viewModel.saveNote(noteId, title, bodyField.text, colorTag, folderId, tags)
            if (pinned != (note?.pinned ?: false)) viewModel.togglePin(noteId)
            if (favorite != (note?.favorite ?: false)) viewModel.toggleFavorite(noteId)
            if (locked != (note?.locked ?: false)) viewModel.toggleLock(noteId)
            if (archived != (note?.archivedAt != null)) viewModel.toggleArchive(noteId)
        }
        onClose()
    }

    fun wrapSelection(prefix: String, suffix: String = prefix) {
        val text = bodyField.text
        val sel = bodyField.selection
        if (sel.collapsed) {
            val newText = text.substring(0, sel.start) + prefix + suffix + text.substring(sel.start)
            bodyField = TextFieldValue(newText, androidx.compose.ui.text.TextRange(sel.start + prefix.length))
        } else {
            val selected = text.substring(sel.min, sel.max)
            val newText = text.substring(0, sel.min) + prefix + selected + suffix + text.substring(sel.max)
            bodyField = TextFieldValue(
                newText,
                androidx.compose.ui.text.TextRange(sel.min + prefix.length, sel.min + prefix.length + selected.length)
            )
        }
    }

    fun prefixCurrentLine(prefix: String) {
        val text = bodyField.text
        val cursor = bodyField.selection.start
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val newText = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        bodyField = TextFieldValue(newText, androidx.compose.ui.text.TextRange(cursor + prefix.length))
    }

    val pageColor = noteCardColor(colorTag)

    Scaffold(
        containerColor = pageColor,
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { haptics.click(); onClose() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel and go back")
                    }
                },
                actions = {
                    IconButton(onClick = { haptics.click(); previewMode = !previewMode }) {
                        Icon(
                            imageVector = if (previewMode) Icons.Outlined.EditNote else Icons.Filled.RemoveRedEye,
                            contentDescription = if (previewMode) "Edit" else "Preview"
                        )
                    }
                    IconButton(onClick = { haptics.click(); favorite = !favorite }) {
                        Icon(
                            imageVector = if (favorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (favorite) "Unfavorite" else "Favorite",
                            tint = if (favorite) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { haptics.click(); locked = !locked }) {
                        Icon(
                            imageVector = if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = if (locked) "Locked in vault" else "Add to vault",
                            tint = if (locked) MaterialTheme.colorScheme.primary else LocalContentColor.current
                        )
                    }
                    IconButton(onClick = { haptics.click(); showColorPicker = true }) {
                        Icon(Icons.Filled.Palette, contentDescription = "Note color")
                    }
                    IconButton(onClick = { haptics.click(); showDeleteConfirm = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete note")
                    }
                    IconButton(
                        onClick = { haptics.confirm(); handleSave() },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "Save note")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = pageColor)
            )
        },
        bottomBar = {
            if (!previewMode) {
                FormattingToolbar(
                    containerColor = pageColor,
                    onBold = { wrapSelection("**") },
                    onItalic = { wrapSelection("_") },
                    onUnderline = { wrapSelection("__") },
                    onStrike = { wrapSelection("~~") },
                    onBullet = { prefixCurrentLine("- ") },
                    onNumbered = { prefixCurrentLine("1. ") },
                    onCheckbox = { prefixCurrentLine("- [ ] ") },
                    onQuote = { prefixCurrentLine("> ") }
                )
            }
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            TextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(titleFocusRequester),
                placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                singleLine = true,
                colors = transparentFieldColors(),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next),
            )

            NoteMetaRow(
                folderName = viewModel.folderById(folderId)?.name,
                tags = tags,
                archived = archived,
                onFolderClick = { showFolderPicker = true },
                onTagClick = { showTagEditor = true },
                onArchiveToggle = { archived = !archived }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(Modifier.height(4.dp))

            if (previewMode) {
                MarkdownPreview(text = bodyField.text, modifier = Modifier.fillMaxWidth())
            } else {
                TextField(
                    value = bodyField,
                    onValueChange = { bodyField = it },
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 300.dp),
                    placeholder = { Text("Start writing…", style = MaterialTheme.typography.bodyLarge) },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = transparentFieldColors(),
                )
            }
        }
    }

    if (showColorPicker) {
        ColorPickerSheet(
            selected = colorTag,
            onSelect = { colorTag = it },
            onDismiss = { showColorPicker = false }
        )
    }

    if (showFolderPicker) {
        FolderPickerSheet(
            viewModel = viewModel,
            selected = folderId,
            onSelect = { folderId = it },
            onDismiss = { showFolderPicker = false }
        )
    }

    if (showTagEditor) {
        TagEditorSheet(
            viewModel = viewModel,
            selectedTags = tags,
            onTagsChange = { tags = it },
            onDismiss = { showTagEditor = false }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Move this note to trash?") },
            text = { Text("You can restore it from the trash later.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        haptics.confirm()
                        viewModel.trashNote(noteId)
                        showDeleteConfirm = false
                        onClose()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun NoteMetaRow(
    folderName: String?,
    tags: List<String>,
    archived: Boolean,
    onFolderClick: () -> Unit,
    onTagClick: () -> Unit,
    onArchiveToggle: () -> Unit
) {
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AssistChip(
            onClick = { haptics.click(); onFolderClick() },
            label = { Text(folderName ?: "Folder") },
            leadingIcon = { Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
        AssistChip(
            onClick = { haptics.click(); onTagClick() },
            label = { Text(if (tags.isEmpty()) "Tags" else tags.joinToString(", ")) },
            leadingIcon = { Icon(Icons.Filled.Sell, contentDescription = null, modifier = Modifier.size(16.dp)) }
        )
        AssistChip(
            onClick = { haptics.click(); onArchiveToggle() },
            label = { Text(if (archived) "Archived" else "Archive") },
            leadingIcon = {
                Icon(
                    imageVector = if (archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        )
    }
}

@Composable
private fun MarkdownPreview(text: String, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    Column(modifier) {
        if (text.isBlank()) {
            Text(
                "Nothing to preview yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        text.split("\n").forEach { rawLine ->
            val line = parseMarkdownLine(rawLine)
            when (line.kind) {
                LineKind.BLANK -> Spacer(Modifier.height(10.dp))
                LineKind.HEADING1 -> Text(
                    inlineMarkdownToAnnotatedString(line.content, color),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                LineKind.HEADING2 -> Text(
                    inlineMarkdownToAnnotatedString(line.content, color),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                LineKind.HEADING3 -> Text(
                    inlineMarkdownToAnnotatedString(line.content, color),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                LineKind.BULLET -> Row {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(inlineMarkdownToAnnotatedString(line.content, color), style = MaterialTheme.typography.bodyLarge)
                }
                LineKind.NUMBERED -> Row {
                    Text("•  ", style = MaterialTheme.typography.bodyLarge)
                    Text(inlineMarkdownToAnnotatedString(line.content, color), style = MaterialTheme.typography.bodyLarge)
                }
                LineKind.CHECKBOX -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (line.checked) Icons.Filled.Check else Icons.Outlined.CheckBoxOutlineBlank,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = if (line.checked) MaterialTheme.colorScheme.primary else color
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        inlineMarkdownToAnnotatedString(line.content, color),
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (line.checked) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                        color = if (line.checked) color.copy(alpha = 0.5f) else color
                    )
                }
                LineKind.QUOTE -> Surface(
                    color = color.copy(alpha = 0.06f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        inlineMarkdownToAnnotatedString(line.content, color),
                        style = MaterialTheme.typography.bodyLarge.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
                LineKind.DIVIDER -> HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                LineKind.PLAIN -> Text(
                    inlineMarkdownToAnnotatedString(line.content, color),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun transparentFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    disabledIndicatorColor = Color.Transparent,
)

@Composable
private fun FormattingToolbar(
    containerColor: Color,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onUnderline: () -> Unit,
    onStrike: () -> Unit,
    onBullet: () -> Unit,
    onNumbered: () -> Unit,
    onCheckbox: () -> Unit,
    onQuote: () -> Unit
) {
    Surface(color = containerColor, tonalElevation = 2.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 4.dp, vertical = 4.dp)
                .navigationBarsPadding(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarIcon(Icons.Filled.FormatBold, "Bold", onBold)
            ToolbarIcon(Icons.Filled.FormatItalic, "Italic", onItalic)
            ToolbarIcon(Icons.Filled.FormatUnderlined, "Underline", onUnderline)
            ToolbarIcon(Icons.Filled.FormatStrikethrough, "Strikethrough", onStrike)
            ToolbarIcon(Icons.AutoMirrored.Filled.FormatListBulleted, "Bullet list", onBullet)
            ToolbarIcon(Icons.Filled.FormatListNumbered, "Numbered list", onNumbered)
            ToolbarIcon(Icons.Outlined.CheckBoxOutlineBlank, "Checklist", onCheckbox)
            ToolbarIcon(Icons.Filled.FormatQuote, "Quote", onQuote)
        }
    }
}

@Composable
private fun ToolbarIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    IconButton(onClick = { haptics.click(); onClick() }) {
        Icon(icon, contentDescription = label)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorPickerSheet(
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Note color", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                NoteColor.entries.forEach { color ->
                    val swatch = noteCardColor(color.name)
                    val isSelected = color.name == selected
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(swatch, CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { haptics.click(); onSelect(color.name); onDismiss() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FolderPickerSheet(
    viewModel: NoteViewModel,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var newFolderName by remember { mutableStateOf("") }
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Move to folder", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            FolderRow("No folder", selected == null) { haptics.click(); onSelect(null); onDismiss() }
            viewModel.folders.value.forEach { folder ->
                FolderRow(folder.name, selected == folder.id) { haptics.click(); onSelect(folder.id); onDismiss() }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("New folder") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    haptics.click()
                    if (newFolderName.isNotBlank()) {
                        val id = viewModel.createFolder(newFolderName)
                        onSelect(id)
                        newFolderName = ""
                        onDismiss()
                    }
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Create folder")
                }
            }
        }
    }
}

@Composable
private fun FolderRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Folder, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(name)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagEditorSheet(
    viewModel: NoteViewModel,
    selectedTags: List<String>,
    onTagsChange: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var newTag by remember { mutableStateOf("") }
    var current by remember { mutableStateOf(selectedTags) }
    val existingTags = viewModel.allTags()
    val haptics = rememberHaptics(LocalHapticsEnabled.current)

    ModalBottomSheet(onDismissRequest = { onTagsChange(current); onDismiss() }) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp)) {
            Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            if (current.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(current) { tag ->
                        InputChip(
                            selected = true,
                            onClick = { haptics.click(); current = current - tag },
                            label = { Text(tag) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Remove", modifier = Modifier.size(14.dp)) }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTag,
                    onValueChange = { newTag = it },
                    placeholder = { Text("New tag") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = {
                    haptics.click()
                    val t = newTag.trim()
                    if (t.isNotEmpty() && t !in current) current = current + t
                    newTag = ""
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add tag")
                }
            }
            val suggestions = existingTags.filter { it !in current }
            if (suggestions.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Suggestions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(suggestions) { tag ->
                        AssistChip(onClick = { haptics.click(); current = current + tag }, label = { Text(tag) })
                    }
                }
            }
        }
    }
}

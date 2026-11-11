package com.rynekryz.rynotes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private class LicenseEntry(val title: String, val subtitle: String, val rawRes: Int)

private val licenseEntries = listOf(
    LicenseEntry("RyNotes", "GNU General Public License v3.0 or later", R.raw.license_gpl3),
    LicenseEntry("AndroidX & Jetpack libraries", "activity-compose, biometric, fragment, lifecycle, datastore-preferences · Apache License 2.0", R.raw.license_apache2),
    LicenseEntry("Jetpack Compose & Material 3", "by Google · Apache License 2.0", R.raw.license_apache2),
    LicenseEntry("Material Icons", "by Google · Apache License 2.0", R.raw.license_apache2),
    LicenseEntry("Kotlin standard library", "by JetBrains · Apache License 2.0", R.raw.license_apache2)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicensesScreen(
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    var expandedIndex by rememberSaveable { mutableStateOf(-1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Open source licenses", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { haptics.click(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "This app is free and open source software. The libraries it uses are licensed as follows. Tap a license to view its full text.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, bottom = 12.dp)
            )
            licenseEntries.forEachIndexed { index, entry ->
                val shape = when (index) {
                    0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                    licenseEntries.lastIndex -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                    else -> RoundedCornerShape(4.dp)
                }
                var expanded by rememberSaveable(index) { mutableStateOf(false) }
                Surface(
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptics.click()
                                    expanded = !expanded
                                    expandedIndex = if (expanded) index else -1
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    entry.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                contentDescription = if (expanded) "Collapse" else "Expand",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (expanded) {
                            val text = remember(index) { context.resources.openRawResource(entry.rawRes).bufferedReader().readText() }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                            Text(
                                text = text,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .heightIn(max = 320.dp)
                                    .verticalScroll(rememberScrollState())
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

package com.rynekryz.rynotes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
    onOpenNote: (String) -> Unit,
    onSetUpVault: () -> Unit,
) {
    var unlocked by remember { mutableStateOf(value = false) }
    var pin by remember { mutableStateOf(value = "") }
    var secret by remember { mutableStateOf(value = "") }
    var error by remember { mutableStateOf(value = false) }
    val haptics = rememberHaptics(viewModel.hapticsEnabled)
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    fun attempt(value: String) {
        if (viewModel.tryUnlockVault(value)) {
            unlocked = true
            error = false
            haptics.confirm()
        } else {
            error = true
            pin = ""
            haptics.reject()
        }
    }

    fun tryBiometric() {
        if ((activity == null || !viewModel.vaultBiometricEnabled)) return
        BiometricAuth.prompt(
            activity = activity,
            title = "Unlock vault",
            subtitle = "Use your fingerprint to continue",
            onSuccess = {
                haptics.confirm()
                if (viewModel.unlockVaultWithBiometrics()) unlocked = true
            }
        )
    }

    LaunchedEffect(viewModel.vaultConfigured) {
        if (viewModel.vaultConfigured) tryBiometric()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vault", fontWeight = FontWeight.SemiBold) },
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
        when {
            !viewModel.vaultConfigured -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Set up your vault", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Protect sensitive notes with their own password",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(onClick = { haptics.click(); onSetUpVault() }) { Text("Set up vault") }
                }
            }
            !unlocked -> {
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Filled.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(20.dp))
                    Text(
                        when (viewModel.vaultType) {
                            LockType.PIN -> "Enter vault PIN"
                            LockType.PATTERN -> "Draw vault pattern"
                            LockType.PASSWORD -> "Enter vault password"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    if (error) {
                        Text("Incorrect, try again", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                    }
                    when (viewModel.vaultType) {
                        LockType.PIN -> {
                            PinDots(length = PIN_LENGTH, filled = pin.length)
                            Spacer(Modifier.height(24.dp))
                            PinPad(
                                onDigit = { d ->
                                    if (pin.length < PIN_LENGTH) pin += d
                                    if (pin.length >= PIN_LENGTH) {
                                        val toTry = pin
                                        attempt(toTry)
                                    }
                                }
                            ) { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                        }
                        LockType.PATTERN -> {
                            PatternPad(onPatternComplete = { pattern -> attempt(pattern) })
                        }
                        LockType.PASSWORD -> {
                            PasswordField(value = secret, onValueChange = { secret = it }, label = "Vault password")
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { haptics.click(); attempt(secret) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Unlock") }
                        }
                    }
                    if (viewModel.vaultBiometricEnabled) {
                        Spacer(Modifier.height(20.dp))
                        TextButton(onClick = { haptics.click(); tryBiometric() }) {
                            Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Use fingerprint")
                        }
                    }
                }
            }
            else -> {
                val vaultNotes = viewModel.vaultNotes()
                if (vaultNotes.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize().padding(padding).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(56.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("No locked notes", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Lock a note from the editor to add it here",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(12.dp),
                        verticalItemSpacing = 10.dp,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(vaultNotes, key = { it.id }) { note ->
                            NoteGridCard(note = note, onClick = { onOpenNote(note.id) })
                        }
                    }
                }
            }
        }
    }
}

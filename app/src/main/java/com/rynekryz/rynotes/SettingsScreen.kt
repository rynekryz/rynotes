package com.rynekryz.rynotes

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.core.net.toUri
import com.rynekryz.rynotes.BuildConfig
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NoteViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember(activity) { activity?.let { BiometricAuth.isAvailable(it) } ?: false }
    val scope = rememberCoroutineScope()
    val topHaptics = rememberHaptics(viewModel.hapticsEnabled)
    val snackbarHostState = remember { SnackbarHostState() }
    var showCredits by remember { mutableStateOf(value = false) }
    var showAppLockSetup by remember { mutableStateOf(value = false) }
    var showVaultSetup by remember { mutableStateOf(value = false) }
    var showDisableLockConfirm by remember { mutableStateOf<DisableTarget?>(value = null) }
    var showRestoreConfirm by remember { mutableStateOf<Uri?>(value = null) }
    var isWorkingOnBackup by remember { mutableStateOf(value = false) }
    var pendingVerify by remember { mutableStateOf<PendingVerify?>(value = null) }
    var verifiedSecret by remember { mutableStateOf<String?>(value = null) }
    var verifiedByBiometric by remember { mutableStateOf(value = false) }

    fun requireVerification(target: DisableTarget, onVerified: () -> Unit) {
        val biometricEnabled = if (target == DisableTarget.APP_LOCK) viewModel.appLockBiometricEnabled else viewModel.vaultBiometricEnabled
        if ((biometricEnabled && activity != null)) {
            BiometricAuth.prompt(
                activity = activity,
                title = if (target == DisableTarget.APP_LOCK) "Confirm app lock" else "Confirm vault password",
                subtitle = "Verify it's you to continue",
                onSuccess = {
                    verifiedByBiometric = true
                    verifiedSecret = null
                    onVerified()
                }
            ) { _ -> pendingVerify = PendingVerify(target, onVerified) }
        } else {
            pendingVerify = PendingVerify(target, onVerified)
        }
    }

    val createBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isWorkingOnBackup = true
        scope.launch {
            try {
                val json = viewModel.exportBackupJson()
                BackupManager.writeBackupZip(context, uri, json)
                snackbarHostState.showSnackbar("Backup saved")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Backup failed: ${e.message ?: "unknown error"}")
            } finally {
                isWorkingOnBackup = false
            }
        }
    }

    val restorePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { showRestoreConfirm = it }
    }

    fun performRestore(uri: Uri) {
        isWorkingOnBackup = true
        scope.launch {
            try {
                val json = BackupManager.readBackupZip(context, uri)
                when (val result = viewModel.restoreFromBackupJson(json)) {
                    is NoteViewModel.RestoreResult.Success -> snackbarHostState.showSnackbar("Backup restored")
                    is NoteViewModel.RestoreResult.Failure -> snackbarHostState.showSnackbar("Restore failed: ${result.message}")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Restore failed: ${e.message ?: "invalid backup file"}")
            } finally {
                isWorkingOnBackup = false
            }
        }
    }

    val versionName = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull() ?: "1.0"
    }
    val versionCode = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode
        }.getOrNull() ?: 1
    }
    val buildTypeLabel = remember { if (BuildConfig.DEBUG) "debug" else "release" }

    fun openUrl(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) }
    }

    Scaffold(
        topBar = {
            MediumTopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { topHaptics.click(); onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            SettingsGroupLabel("Appearance")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Filled.DarkMode,
                    title = "Dark mode",
                    subtitle = "Use a dark theme across the app",
                    roundingType = RoundingType.TOP,
                    trailingContent = {
                        Switch(
                            checked = viewModel.darkModeEnabled,
                            onCheckedChange = viewModel::updateDarkMode
                        )
                    },
                    onClick = { viewModel.updateDarkMode(!viewModel.darkModeEnabled) }
                )
                if (viewModel.darkModeEnabled) {
                    SettingsItem(
                        icon = Icons.Filled.Contrast,
                        title = "Pure dark mode",
                        subtitle = "Use pure black for AMOLED screens",
                        roundingType = RoundingType.MIDDLE,
                        trailingContent = {
                            Switch(
                                checked = viewModel.pureDarkMode,
                                onCheckedChange = viewModel::updatePureDark
                            )
                        },
                        onClick = { viewModel.updatePureDark(!viewModel.pureDarkMode) }
                    )
                }
                SettingsItem(
                    icon = Icons.Filled.Brightness6,
                    title = "Dynamic color",
                    subtitle = "Match colors to your wallpaper",
                    roundingType = RoundingType.BOTTOM,
                    trailingContent = {
                        Switch(
                            checked = viewModel.dynamicColorEnabled,
                            onCheckedChange = viewModel::updateDynamicColor
                        )
                    },
                    onClick = { viewModel.updateDynamicColor(!viewModel.dynamicColorEnabled) }
                )
            }

            SettingsGroupLabel("Text")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Filled.Vibration,
                    title = "Haptic feedback",
                    subtitle = "Vibrate on taps and actions",
                    roundingType = RoundingType.TOP,
                    trailingContent = {
                        Switch(
                            checked = viewModel.hapticsEnabled,
                            onCheckedChange = viewModel::updateHaptics
                        )
                    },
                    onClick = { viewModel.updateHaptics(!viewModel.hapticsEnabled) }
                )
                SettingsItem(
                    icon = Icons.Filled.TextFields,
                    title = "Use system font",
                    subtitle = if (viewModel.useSystemFont) "Using your device's default font" else "Using Google Sans Flex",
                    roundingType = RoundingType.MIDDLE,
                    trailingContent = {
                        Switch(
                            checked = viewModel.useSystemFont,
                            onCheckedChange = viewModel::updateUseSystemFont
                        )
                    },
                    onClick = { viewModel.updateUseSystemFont(!viewModel.useSystemFont) }
                )
                Surface(
                    shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("Font size", fontWeight = FontWeight.SemiBold)
                                Text(
                                    "${(viewModel.fontScale * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Slider(
                            value = viewModel.fontScale,
                            onValueChange = viewModel::updateFontScale,
                            valueRange = 0.8f..1.4f,
                            steps = 5
                        )
                        Text(
                            "The quick brown fox jumps",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = MaterialTheme.typography.bodyLarge.fontSize * viewModel.fontScale
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            SettingsGroupLabel("Security")
            SettingsGroup {
                SettingsItem(
                    icon = if (viewModel.hasAppLock) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    title = "App lock",
                    subtitle = if (viewModel.hasAppLock) "Locked with ${viewModel.appLockType.name.lowercase()}" else "Require a PIN, pattern, or password to open the app",
                    roundingType = if (viewModel.hasAppLock) RoundingType.TOP else RoundingType.SINGLE,
                    trailingContent = {
                        Switch(
                            checked = viewModel.hasAppLock,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    showAppLockSetup = true
                                } else {
                                    requireVerification(DisableTarget.APP_LOCK) { showDisableLockConfirm = DisableTarget.APP_LOCK }
                                }
                            }
                        )
                    },
                    onClick = {
                        if (viewModel.hasAppLock) {
                            requireVerification(DisableTarget.APP_LOCK) { showDisableLockConfirm = DisableTarget.APP_LOCK }
                        } else {
                            showAppLockSetup = true
                        }
                    }
                )
                if (viewModel.hasAppLock) {
                    SettingsItem(
                        icon = Icons.Filled.Lock,
                        title = "Change app lock",
                        subtitle = "Update your PIN, pattern, or password",
                        roundingType = if (biometricAvailable) RoundingType.MIDDLE else RoundingType.BOTTOM,
                        onClick = { requireVerification(DisableTarget.APP_LOCK) { showAppLockSetup = true } }
                    )
                    if (biometricAvailable) {
                        SettingsItem(
                            icon = Icons.Filled.Fingerprint,
                            title = "Unlock app with fingerprint",
                            subtitle = "Use your device's fingerprint sensor instead",
                            roundingType = RoundingType.BOTTOM,
                            trailingContent = {
                                Switch(
                                    checked = viewModel.appLockBiometricEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) viewModel.updateAppLockBiometricEnabled(true)
                                        else requireVerification(DisableTarget.APP_LOCK) { viewModel.updateAppLockBiometricEnabled(false) }
                                    }
                                )
                            },
                            onClick = {
                                if (viewModel.appLockBiometricEnabled) {
                                    requireVerification(DisableTarget.APP_LOCK) { viewModel.updateAppLockBiometricEnabled(false) }
                                } else {
                                    viewModel.updateAppLockBiometricEnabled(true)
                                }
                            }
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                SettingsItem(
                    icon = Icons.Filled.Shield,
                    title = "Vault password",
                    subtitle = if (viewModel.vaultConfigured) "Change or remove vault protection" else "Set up a password to lock individual notes",
                    roundingType = if (viewModel.vaultConfigured && biometricAvailable) RoundingType.TOP else RoundingType.SINGLE,
                    onClick = {
                        if (viewModel.vaultConfigured) {
                            requireVerification(DisableTarget.VAULT) { showVaultSetup = true }
                        } else {
                            showVaultSetup = true
                        }
                    }
                )
                if (viewModel.vaultConfigured && biometricAvailable) {
                    SettingsItem(
                        icon = Icons.Filled.Fingerprint,
                        title = "Unlock vault with fingerprint",
                        subtitle = "Use your device's fingerprint sensor instead",
                        roundingType = RoundingType.BOTTOM,
                        trailingContent = {
                            Switch(
                                checked = viewModel.vaultBiometricEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) viewModel.updateVaultBiometricEnabled(true)
                                    else requireVerification(DisableTarget.VAULT) { viewModel.updateVaultBiometricEnabled(false) }
                                }
                            )
                        },
                        onClick = {
                            if (viewModel.vaultBiometricEnabled) {
                                requireVerification(DisableTarget.VAULT) { viewModel.updateVaultBiometricEnabled(false) }
                            } else {
                                viewModel.updateVaultBiometricEnabled(true)
                            }
                        }
                    )
                }
            }

            SettingsGroupLabel("Data")
            SettingsGroup {
                SettingsItem(
                    icon = Icons.Filled.CloudUpload,
                    title = "Backup",
                    subtitle = "Save all notes, folders, and settings to a .zip file",
                    roundingType = RoundingType.TOP,
                    onClick = { if (!isWorkingOnBackup) createBackupLauncher.launch(BackupManager.defaultFileName()) }
                )
                SettingsItem(
                    icon = Icons.Filled.CloudDownload,
                    title = "Restore",
                    subtitle = "Replace current data with a backup .zip file",
                    roundingType = RoundingType.BOTTOM,
                    onClick = { if (!isWorkingOnBackup) restorePickerLauncher.launch(arrayOf("application/zip", "application/octet-stream")) }
                )
            }

            SettingsGroupLabel("About")
            SettingsGroup {
                val aboutHaptics = rememberHaptics(viewModel.hapticsEnabled)
                SettingsItem(
                    icon = Icons.Filled.Favorite,
                    title = "RyNotes",
                    subtitle = "v$versionName ($versionCode) · $buildTypeLabel · by Ryne",
                    roundingType = RoundingType.TOP,
                    onClick = { aboutHaptics.click(); openUrl("https://github.com/rynekryz") }
                )
                SettingsItem(
                    icon = Icons.Filled.Info,
                    title = "Credits",
                    subtitle = "Open source libraries and design credits",
                    roundingType = RoundingType.MIDDLE,
                    onClick = { showCredits = true }
                )
                SettingsItem(
                    icon = Icons.Filled.Balance,
                    title = "License",
                    subtitle = "GNU General Public License v3.0 (GPLv3)\nFree, Open source software. You may use, study, share and improve this software.",
                    roundingType = RoundingType.BOTTOM,
                    onClick = { openUrl("https://github.com/rynekryz/rynotes?tab=GPL-3.0-1-ov-file") }
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showCredits) {
        CreditsDialog(onDismiss = { showCredits = false }, onOpenUrl = ::openUrl)
    }

    if (showAppLockSetup) {
        LockSetupDialog(
            title = "Set up app lock",
            onDismiss = { showAppLockSetup = false; verifiedSecret = null; verifiedByBiometric = false },
            onConfirm = { type, secret ->
                if (viewModel.hasAppLock) {
                    val current = verifiedSecret
                    when {
                        verifiedByBiometric -> viewModel.changeAppLockWithBiometrics(type, secret)
                        current != null -> viewModel.changeAppLock(current, type, secret)
                    }
                } else {
                    viewModel.setAppLock(true, type, secret)
                }
                showAppLockSetup = false
                verifiedSecret = null
                verifiedByBiometric = false
            }
        )
    }

    if (showVaultSetup) {
        LockSetupDialog(
            title = if (viewModel.vaultConfigured) "Change vault password" else "Set up vault",
            onDismiss = { showVaultSetup = false; verifiedSecret = null; verifiedByBiometric = false },
            onConfirm = { type, secret ->
                if (viewModel.vaultConfigured) {
                    val current = verifiedSecret
                    when {
                        verifiedByBiometric -> viewModel.changeVaultSecretWithBiometrics(type, secret)
                        current != null -> viewModel.changeVaultSecret(current, type, secret)
                    }
                } else {
                    viewModel.setVaultSecret(type, secret)
                }
                showVaultSetup = false
                verifiedSecret = null
                verifiedByBiometric = false
            },
            extraAction = if (viewModel.vaultConfigured) {
                { showVaultSetup = false; showDisableLockConfirm = DisableTarget.VAULT }
            } else null,
            extraActionLabel = "Remove vault"
        )
    }

    showDisableLockConfirm?.let { target ->
        AlertDialog(
            onDismissRequest = { showDisableLockConfirm = null; verifiedSecret = null; verifiedByBiometric = false },
            title = { Text(if (target == DisableTarget.APP_LOCK) "Turn off app lock?" else "Remove vault?") },
            text = {
                Text(
                    if (target == DisableTarget.APP_LOCK) "Anyone will be able to open RyNotes without a PIN, pattern, or password."
                    else "Locked notes will be unlocked and moved back to your regular notes."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val secret = verifiedSecret
                        val ok = when (target) {
                            DisableTarget.APP_LOCK -> when {
                                verifiedByBiometric -> viewModel.disableAppLockWithBiometrics()
                                secret != null -> viewModel.disableAppLock(secret)
                                else -> false
                            }
                            DisableTarget.VAULT -> when {
                                verifiedByBiometric -> viewModel.removeVaultWithBiometrics()
                                secret != null -> viewModel.removeVault(secret)
                                else -> false
                            }
                        }
                        if (!ok) {
                            scope.launch { snackbarHostState.showSnackbar("Couldn't verify, try again") }
                        }
                        showDisableLockConfirm = null
                        verifiedSecret = null
                        verifiedByBiometric = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { showDisableLockConfirm = null; verifiedSecret = null; verifiedByBiometric = false }) { Text("Cancel") }
            }
        )
    }

    pendingVerify?.let { pending ->
        VerifySecretDialog(
            title = if (pending.target == DisableTarget.APP_LOCK) "Confirm app lock" else "Confirm vault password",
            lockType = if (pending.target == DisableTarget.APP_LOCK) viewModel.appLockType else viewModel.vaultType,
            onVerify = { secret ->
                val ok = if (pending.target == DisableTarget.APP_LOCK) {
                    viewModel.verifyAppLockSecret(secret)
                } else {
                    viewModel.tryUnlockVault(secret)
                }
                if (ok) {
                    verifiedSecret = secret
                    verifiedByBiometric = false
                    pendingVerify = null
                    pending.onVerified()
                }
                ok
            },
            onDismiss = { pendingVerify = null }
        )
    }

    showRestoreConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = null },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null) },
            title = { Text("Restore this backup?") },
            text = {
                Text("This replaces all current notes, folders, and settings — including app lock and vault — with what's in the backup file. This can't be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreConfirm = null
                        performRestore(uri)
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = null }) { Text("Cancel") }
            }
        )
    }
}

private enum class DisableTarget { APP_LOCK, VAULT }

private data class PendingVerify(val target: DisableTarget, val onVerified: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VerifySecretDialog(
    title: String,
    lockType: LockType,
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var pin by remember { mutableStateOf(value = "") }
    var secret by remember { mutableStateOf(value = "") }
    var error by remember { mutableStateOf(value = false) }
    val haptics = rememberHaptics(LocalHapticsEnabled.current)

    fun attempt(value: String) {
        if (onVerify(value)) {
            haptics.confirm()
        } else {
            error = true
            pin = ""
            haptics.reject()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(
                    when (lockType) {
                        LockType.PIN -> "Enter your current PIN"
                        LockType.PATTERN -> "Draw your current pattern"
                        LockType.PASSWORD -> "Enter your current password"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(12.dp))
                if (error) {
                    Text("Incorrect, try again", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                when (lockType) {
                    LockType.PIN -> {
                        PinDots(length = PIN_LENGTH, filled = pin.length)
                        Spacer(Modifier.height(20.dp))
                        PinPad(
                            onDigit = { d ->
                                if (pin.length < PIN_LENGTH) pin += d
                                if (pin.length >= PIN_LENGTH) attempt(pin)
                            },
                            onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                        )
                    }
                    LockType.PATTERN -> {
                        PatternPad(onPatternComplete = { pattern -> attempt(pattern) })
                    }
                    LockType.PASSWORD -> {
                        PasswordField(value = secret, onValueChange = { secret = it }, label = "Current password")
                    }
                }
            }
        },
        confirmButton = {
            if (lockType == LockType.PASSWORD) {
                TextButton(onClick = { attempt(secret) }) { Text("Verify") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun SettingsGroupLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 8.dp, top = 24.dp, bottom = 12.dp)
    )
}

private enum class RoundingType { TOP, MIDDLE, BOTTOM, SINGLE }

@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    roundingType: RoundingType = RoundingType.SINGLE,
    onClick: () -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null
) {
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    val shape = when (roundingType) {
        RoundingType.TOP -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        RoundingType.BOTTOM -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
        RoundingType.MIDDLE -> RoundedCornerShape(4.dp)
        RoundingType.SINGLE -> RoundedCornerShape(24.dp)
    }
    Surface(
        onClick = {
            haptics.click()
            onClick()
        },
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        ListItem(
            headlineContent = { Text(title, fontWeight = FontWeight.SemiBold) },
            supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall) },
            leadingContent = {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            },
            trailingContent = trailingContent,
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}

@Composable
private fun <T> SingleChoiceSegmented(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Surface(
                onClick = { onSelect(value) },
                shape = RoundedCornerShape(50),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp)) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private data class CreditEntry(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val url: String
)

private val credits = listOf(
    CreditEntry(Icons.Filled.Info, "Material Design Icons", "by Google · Apache License 2.0", "https://fonts.google.com/icons"),
    CreditEntry(Icons.Filled.Contrast, "Material You Expressive", "by Google", "https://m3.material.io/blog/building-with-m3-expressive"),
    CreditEntry(Icons.Filled.PrivacyTip, "Jetpack DataStore", "by Google · Apache License 2.0", "https://developer.android.com/topic/libraries/architecture/datastore"),
    CreditEntry(Icons.Filled.Balance, "Kotlin", "by JetBrains · Apache License 2.0", "https://kotlinlang.org")
)

@Composable
private fun CreditsDialog(onDismiss: () -> Unit, onOpenUrl: (String) -> Unit) {
    val haptics = rememberHaptics(LocalHapticsEnabled.current)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Credits") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                credits.forEachIndexed { index, entry ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(entry.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(entry.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text(entry.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { haptics.click(); onOpenUrl(entry.url) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open link", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun LockSetupDialog(
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (LockType, String) -> Unit,
    extraAction: (() -> Unit)? = null,
    extraActionLabel: String = ""
) {
    var selectedType by remember { mutableStateOf(value = LockType.PIN) }
    var pin by remember { mutableStateOf(value = "") }
    var confirmPin by remember { mutableStateOf(value = "") }
    var password by remember { mutableStateOf(value = "") }
    var confirmPassword by remember { mutableStateOf(value = "") }
    var pattern by remember { mutableStateOf(value = "") }
    var confirmPattern by remember { mutableStateOf<String?>(value = null) }
    var errorText by remember { mutableStateOf<String?>(value = null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                SingleChoiceSegmented(
                    options = listOf(LockType.PIN to "PIN", LockType.PATTERN to "Pattern", LockType.PASSWORD to "Password"),
                    selected = selectedType,
                    onSelect = {
                        selectedType = it
                        errorText = null
                        pin = ""; confirmPin = ""; password = ""; confirmPassword = ""; pattern = ""; confirmPattern = null
                    }
                )
                Spacer(Modifier.height(16.dp))
                errorText?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                }
                when (selectedType) {
                    LockType.PIN -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            val enteringConfirm = pin.length >= PIN_LENGTH
                            Text(
                                if (!enteringConfirm) "Enter exactly $PIN_LENGTH digits"
                                else "Confirm your $PIN_LENGTH-digit PIN",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            PinDots(
                                length = PIN_LENGTH,
                                filled = if (enteringConfirm) confirmPin.length else pin.length
                            )
                            Spacer(Modifier.height(20.dp))
                            PinPad(
                                onDigit = { d ->
                                    if (!enteringConfirm) {
                                        if (pin.length < PIN_LENGTH) pin += d
                                    } else {
                                        if (confirmPin.length < PIN_LENGTH) confirmPin += d
                                    }
                                },
                                onBackspace = {
                                    if (!enteringConfirm) {
                                        if (pin.isNotEmpty()) pin = pin.dropLast(1)
                                    } else {
                                        if (confirmPin.isNotEmpty()) confirmPin = confirmPin.dropLast(1)
                                        else pin = pin.dropLast(1)
                                    }
                                }
                            )
                        }
                    }
                    LockType.PASSWORD -> {
                        PasswordField(value = password, onValueChange = { password = it }, label = "Enter password")
                        Spacer(Modifier.height(10.dp))
                        PasswordField(value = confirmPassword, onValueChange = { confirmPassword = it }, label = "Confirm password")
                    }
                    LockType.PATTERN -> {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    if (confirmPattern == null) "Draw a pattern" else "Confirm your pattern",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(12.dp))
                                PatternPad(onPatternComplete = { drawn ->
                                    if (drawn.length < 4) {
                                        errorText = "Connect at least 4 dots"
                                    } else if (confirmPattern == null) {
                                        pattern = drawn
                                        confirmPattern = ""
                                        errorText = null
                                    } else {
                                        confirmPattern = drawn
                                    }
                                })
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (selectedType) {
                    LockType.PIN -> {
                        if (pin.length != PIN_LENGTH) errorText = "PIN must be exactly $PIN_LENGTH digits"
                        else if (confirmPin.length != PIN_LENGTH) errorText = "Confirm your $PIN_LENGTH-digit PIN"
                        else if (pin != confirmPin) errorText = "PINs don't match"
                        else onConfirm(LockType.PIN, pin)
                    }
                    LockType.PASSWORD -> {
                        if (password.length < 4) errorText = "Password must be at least 4 characters"
                        else if (password != confirmPassword) errorText = "Passwords don't match"
                        else onConfirm(LockType.PASSWORD, password)
                    }
                    LockType.PATTERN -> {
                        if (pattern.isEmpty()) errorText = "Draw a pattern first"
                        else if (confirmPattern != pattern) errorText = "Patterns don't match"
                        else onConfirm(LockType.PATTERN, pattern)
                    }
                }
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                extraAction?.let {
                    TextButton(
                        onClick = it,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) { Text(extraActionLabel) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
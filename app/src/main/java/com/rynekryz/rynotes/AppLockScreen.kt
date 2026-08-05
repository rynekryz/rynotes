package com.rynekryz.rynotes

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@Composable
fun AppLockScreen(viewModel: NoteViewModel) {
    var pin by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val shake = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics(viewModel.hapticsEnabled)
    val context = LocalContext.current
    val activity = context as? FragmentActivity

    fun attempt(secret: String) {
        if (viewModel.tryUnlockApp(secret)) {
            error = false
            haptics.confirm()
        } else {
            error = true
            pin = ""
            haptics.reject()
            scope.launch {
                shake.snapTo(0f)
                shake.animateTo(1f, animationSpec = androidx.compose.animation.core.tween(300))
                shake.snapTo(0f)
            }
        }
    }

    fun tryBiometric() {
        if (activity == null || !viewModel.appLockBiometricEnabled) return
        BiometricAuth.prompt(
            activity = activity,
            title = "Unlock RyNotes",
            subtitle = "Use your fingerprint to continue",
            onSuccess = {
                haptics.confirm()
                viewModel.unlockAppWithBiometrics()
            }
        )
    }

    LaunchedEffect(Unit) { tryBiometric() }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = androidx.compose.foundation.shape.CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text("RyNotes is locked", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                when (viewModel.appLockType) {
                    LockType.PIN -> "Enter your PIN to continue"
                    LockType.PATTERN -> "Draw your pattern to continue"
                    LockType.PASSWORD -> "Enter your password to continue"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            if (error) {
                Text(
                    "Incorrect, try again",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(16.dp))
            }

            when (viewModel.appLockType) {
                LockType.PIN -> {
                    PinDots(length = PIN_LENGTH, filled = pin.length)
                    Spacer(Modifier.height(32.dp))
                    PinPad(
                        onDigit = { d ->
                            if (pin.length < PIN_LENGTH) pin += d
                            if (pin.length >= PIN_LENGTH) {
                                val toTry = pin
                                attempt(toTry)
                            }
                        },
                        onBackspace = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
                    )
                }
                LockType.PATTERN -> {
                    PatternPad(onPatternComplete = { pattern -> attempt(pattern) })
                }
                LockType.PASSWORD -> {
                    PasswordField(value = password, onValueChange = { password = it })
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { haptics.click(); attempt(password) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Unlock")
                    }
                }
            }

            if (viewModel.appLockBiometricEnabled) {
                Spacer(Modifier.height(24.dp))
                TextButton(onClick = { haptics.click(); tryBiometric() }) {
                    Icon(Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Use fingerprint")
                }
            }
        }
    }
}

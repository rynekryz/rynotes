package com.rynekryz.rynotes

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity

private sealed class Screen {
    data object Home : Screen()
    data object Settings : Screen()
    data object Trash : Screen()
    data object Vault : Screen()
    data class Editor(val noteId: String) : Screen()
    data class Pdf(val uri: String) : Screen()
}

private data class ScreenEntry(val screen: Screen, val depth: Int)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)
class MainActivity : FragmentActivity() {
    private val vm: NoteViewModel by viewModels()
    private var pendingPdfUri by mutableStateOf<Uri?>(value = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        enableEdgeToEdge()
        setContent {
            RyNotesTheme(
                darkTheme = vm.darkModeEnabled,
                dynamicColor = vm.dynamicColorEnabled,
                pureDark = vm.pureDarkMode,
                fontScale = vm.fontScale,
                useSystemFont = vm.useSystemFont,
            ) {
                CompositionLocalProvider(LocalHapticsEnabled provides vm.hapticsEnabled) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (vm.isLoading) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                LoadingIndicator()
                            }
                        } else {
                            androidx.compose.animation.Crossfade(
                                targetState = vm.hasAppLock && !vm.appUnlocked,
                                animationSpec = tween(320),
                                label = "appLockGate",
                            ) { locked ->
                                if (locked) {
                                    AppLockScreen(viewModel = vm)
                                } else {
                                    RyNotesApp(vm, pendingPdfUri) { pendingPdfUri = null }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if ((intent?.action == Intent.ACTION_VIEW) && (intent.type == "application/pdf")) {
            pendingPdfUri = intent.data
        }
    }
}

@Composable
private fun RyNotesApp(
    viewModel: NoteViewModel,
    pendingPdfUri: Uri?,
    onPdfConsumed: () -> Unit,
) {
    var backStack by rememberSaveable(stateSaver = BackStackSaver) {
        mutableStateOf(listOf<Screen>(Screen.Home))
    }

    LaunchedEffect(pendingPdfUri) {
        pendingPdfUri?.let { uri ->
            val pdfScreen = Screen.Pdf(uri.toString())
            if (backStack.lastOrNull() != pdfScreen) {
                backStack = backStack + pdfScreen
            }
            onPdfConsumed()
        }
    }

    val current = backStack.last()
    val currentDepth = backStack.size

    fun push(screen: Screen) {
        if (backStack.lastOrNull() == screen) return
        backStack += screen
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    var predictiveProgress by remember { mutableFloatStateOf(0f) }
    var isPredictiveBack by remember { mutableStateOf(value = false) }

    PredictiveBackHandler(enabled = backStack.size > 1) { progressFlow ->
        isPredictiveBack = true
        try {
            progressFlow.collect { event ->
                predictiveProgress = event.progress
            }
            pop()
        } catch (_: Exception) {
        } finally {
            predictiveProgress = 0f
            isPredictiveBack = false
        }
    }

    val settleProgress by animateFloatAsState(
        targetValue = if (isPredictiveBack) predictiveProgress else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
        label = "predictiveBack"
    )

    androidx.compose.animation.AnimatedContent(
        targetState = ScreenEntry(current, currentDepth),
        transitionSpec = {
            val isEnteringEditor = (targetState.screen is Screen.Editor) && (initialState.screen !is Screen.Editor)
            val isLeavingEditor = (initialState.screen is Screen.Editor) && (targetState.screen !is Screen.Editor)
            val isBackNavigation = targetState.depth < initialState.depth

            val bouncySpring = spring<Float>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
            val bouncySpringOffset = spring<androidx.compose.ui.unit.IntOffset>(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
            val exitOffsetSpec = tween<androidx.compose.ui.unit.IntOffset>(durationMillis = 220)

            when {
                isEnteringEditor -> {
                    (androidx.compose.animation.slideInVertically(
                        initialOffsetY = { it / 4 },
                        animationSpec = bouncySpringOffset
                    ) + androidx.compose.animation.scaleIn(
                        initialScale = 0.9f,
                        animationSpec = bouncySpring
                    ) + androidx.compose.animation.fadeIn(animationSpec = tween(260))) togetherWith
                        (androidx.compose.animation.scaleOut(
                            targetScale = 0.94f,
                            animationSpec = tween(220)
                        ) + androidx.compose.animation.fadeOut(animationSpec = tween(180)))
                }
                isLeavingEditor -> {
                    (androidx.compose.animation.scaleIn(
                        initialScale = 0.94f,
                        animationSpec = bouncySpring
                    ) + androidx.compose.animation.fadeIn(animationSpec = tween(240))) togetherWith
                        (androidx.compose.animation.slideOutVertically(
                            targetOffsetY = { it / 4 },
                            animationSpec = exitOffsetSpec
                        ) + androidx.compose.animation.scaleOut(
                            targetScale = 0.9f,
                            animationSpec = tween(220)
                        ) + androidx.compose.animation.fadeOut(animationSpec = tween(180)))
                }
                isBackNavigation -> {
                    (androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { -it / 3 },
                        animationSpec = bouncySpringOffset
                    ) + androidx.compose.animation.fadeIn(animationSpec = tween(260))) togetherWith
                        (androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { it / 5 },
                            animationSpec = exitOffsetSpec
                        ) + androidx.compose.animation.fadeOut(animationSpec = tween(180)))
                }
                else -> {
                    (androidx.compose.animation.slideInHorizontally(
                        initialOffsetX = { it / 3 },
                        animationSpec = bouncySpringOffset
                    ) + androidx.compose.animation.fadeIn(animationSpec = tween(260))) togetherWith
                        (androidx.compose.animation.slideOutHorizontally(
                            targetOffsetX = { -it / 5 },
                            animationSpec = exitOffsetSpec
                        ) + androidx.compose.animation.fadeOut(animationSpec = tween(180)))
                }
            }
        },
        label = "screen"
    ) { entry ->
        val target = entry.screen
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(1f - (settleProgress * 0.08f))
                .alpha(1f - (settleProgress * 0.35f))
        ) {
            when (target) {
                is Screen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { push(Screen.Settings) },
                    onNavigateToTrash = { push(Screen.Trash) },
                    onNavigateToVault = { push(Screen.Vault) },
                    onOpenNote = { id -> push(Screen.Editor(id)) }
                ) { uri -> push(Screen.Pdf(uri.toString())) }
                is Screen.Settings -> SettingsScreen(
                    viewModel = viewModel
                ) { pop() }
                is Screen.Trash -> TrashScreen(
                    viewModel = viewModel
                ) { pop() }
                is Screen.Vault -> VaultScreen(
                    viewModel = viewModel,
                    onBack = { pop() },
                    onOpenNote = { id -> push(Screen.Editor(id)) }
                ) { push(Screen.Settings) }
                is Screen.Editor -> NoteEditorScreen(
                    viewModel = viewModel,
                    noteId = target.noteId
                ) { pop() }
                is Screen.Pdf -> PdfViewerScreen(
                    uri = target.uri.toUri()
                ) { pop() }
            }
        }
    }
}

private val BackStackSaver = androidx.compose.runtime.saveable.Saver<List<Screen>, String>(
    save = { stack -> stack.joinToString("|") { encodeScreen(it) } },
    restore = { value ->
        val parts = value.split("|").filter { it.isNotEmpty() }
        val decoded = parts.map { decodeScreen(it) }
        decoded.ifEmpty { listOf(Screen.Home) }
    }
)

private fun encodeScreen(screen: Screen): String = when (screen) {
    is Screen.Home -> "home"
    is Screen.Settings -> "settings"
    is Screen.Trash -> "trash"
    is Screen.Vault -> "vault"
    is Screen.Editor -> "editor:${screen.noteId}"
    is Screen.Pdf -> "pdf:${screen.uri}"
}

private fun decodeScreen(value: String): Screen = when {
    value == "home" -> Screen.Home
    value == "settings" -> Screen.Settings
    value == "trash" -> Screen.Trash
    value == "vault" -> Screen.Vault
    value.startsWith("editor:") -> Screen.Editor(value.removePrefix("editor:"))
    value.startsWith("pdf:") -> Screen.Pdf(value.removePrefix("pdf:"))
    else -> Screen.Home
}
package com.rynekryz.rynotes

import android.app.Application
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

private val Application.dataStore by preferencesDataStore(name = "rynotes_store")

private val NOTES_KEY = stringPreferencesKey("notes_json")
private val FOLDERS_KEY = stringPreferencesKey("folders_json")
private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode_enabled")
private val DYNAMIC_COLOR_KEY = booleanPreferencesKey("dynamic_color_enabled")
private val PURE_DARK_KEY = booleanPreferencesKey("pure_dark_mode")
private val FONT_SCALE_KEY = floatPreferencesKey("font_scale")
private val USE_SYSTEM_FONT_KEY = booleanPreferencesKey("use_system_font")
private val HAPTICS_ENABLED_KEY = booleanPreferencesKey("haptics_enabled")
private val APP_LOCK_ENABLED_KEY = booleanPreferencesKey("app_lock_enabled")
private val APP_LOCK_TYPE_KEY = stringPreferencesKey("app_lock_type")
private val APP_LOCK_HASH_KEY = stringPreferencesKey("app_lock_hash")
private val VAULT_HASH_KEY = stringPreferencesKey("vault_hash")
private val VAULT_TYPE_KEY = stringPreferencesKey("vault_type")
private val APP_LOCK_BIOMETRIC_KEY = booleanPreferencesKey("app_lock_biometric_enabled")
private val VAULT_BIOMETRIC_KEY = booleanPreferencesKey("vault_biometric_enabled")

const val PIN_LENGTH = 6

private const val TRASH_RETENTION_MILLIS = 30L * 24 * 60 * 60 * 1000

enum class LockType { PIN, PATTERN, PASSWORD }

fun hashSecret(secret: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(secret.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

class NoteViewModel(application: Application) : AndroidViewModel(application) {

    var notes = mutableStateOf<List<Note>>(emptyList())
        private set

    var folders = mutableStateOf<List<NoteFolder>>(emptyList())
        private set

    var isLoading by mutableStateOf(value = true)
        private set

    var darkModeEnabled by mutableStateOf(value = true)
        private set

    var dynamicColorEnabled by mutableStateOf(value = true)
        private set

    var pureDarkMode by mutableStateOf(value = false)
        private set

    var fontScale by mutableFloatStateOf(1f)
        private set

    var useSystemFont by mutableStateOf(value = false)
        private set

    var hapticsEnabled by mutableStateOf(value = true)
        private set

    var appLockEnabled by mutableStateOf(value = false)
        private set

    var appLockType by mutableStateOf(LockType.PIN)
        private set

    private var appLockHash by mutableStateOf<String?>(value = null)

    private var vaultHash by mutableStateOf<String?>(value = null)
    var vaultType by mutableStateOf(LockType.PIN)
        private set

    var appUnlocked by mutableStateOf(value = false)

    var appLockBiometricEnabled by mutableStateOf(value = false)
        private set

    var vaultBiometricEnabled by mutableStateOf(value = false)
        private set

    val hasAppLock: Boolean get() = (appLockEnabled && appLockHash != null)
    val vaultConfigured: Boolean get() = (vaultHash != null)

    init {
        viewModelScope.launch {
            val prefs = getApplication<Application>().dataStore.data.first()
            darkModeEnabled = prefs[DARK_MODE_KEY] ?: true
            pureDarkMode = prefs[PURE_DARK_KEY] ?: false
            dynamicColorEnabled = prefs[DYNAMIC_COLOR_KEY] ?: true
            fontScale = prefs[FONT_SCALE_KEY] ?: 1f
            useSystemFont = prefs[USE_SYSTEM_FONT_KEY] ?: false
            hapticsEnabled = prefs[HAPTICS_ENABLED_KEY] ?: true
            appLockEnabled = prefs[APP_LOCK_ENABLED_KEY] ?: false
            appLockType = runCatching { LockType.valueOf(prefs[APP_LOCK_TYPE_KEY] ?: LockType.PIN.name) }.getOrDefault(LockType.PIN)
            appLockHash = prefs[APP_LOCK_HASH_KEY]
            vaultHash = prefs[VAULT_HASH_KEY]
            vaultType = runCatching { LockType.valueOf(prefs[VAULT_TYPE_KEY] ?: LockType.PIN.name) }.getOrDefault(LockType.PIN)
            appLockBiometricEnabled = (prefs[APP_LOCK_BIOMETRIC_KEY] ?: false) && hasAppLock
            vaultBiometricEnabled = (prefs[VAULT_BIOMETRIC_KEY] ?: false) && vaultHash != null
            appUnlocked = !hasAppLock
            notes.value = purgeExpiredTrash(loadNotes())
            folders.value = loadFolders()
            isLoading = false
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        darkModeEnabled = enabled
        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[DARK_MODE_KEY] = enabled } }
    }

    fun updateDynamicColor(enabled: Boolean) {
        dynamicColorEnabled = enabled
        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[DYNAMIC_COLOR_KEY] = enabled } }
    }

    fun updatePureDark(enabled: Boolean) {
        pureDarkMode = enabled
        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[PURE_DARK_KEY] = enabled } }
    }

    fun updateFontScale(scale: Float) {
        fontScale = scale
        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[FONT_SCALE_KEY] = scale } }
    }

    fun updateUseSystemFont(enabled: Boolean) {
        useSystemFont = enabled
        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[USE_SYSTEM_FONT_KEY] = enabled } }
    }

    fun updateHaptics(enabled: Boolean) {
        hapticsEnabled = enabled
        viewModelScope.launch { getApplication<Application>().dataStore.edit { it[HAPTICS_ENABLED_KEY] = enabled } }
    }

    fun setAppLock(enabled: Boolean, type: LockType, secret: String?) {
        appLockEnabled = enabled
        appLockType = type
        secret?.let { appLockHash = hashSecret(it) }
        if (!enabled) {
            appLockHash = null
            appLockBiometricEnabled = false
        }
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[APP_LOCK_ENABLED_KEY] = enabled
                prefs[APP_LOCK_TYPE_KEY] = type.name
                if (appLockHash != null) prefs[APP_LOCK_HASH_KEY] = appLockHash!! else prefs.remove(APP_LOCK_HASH_KEY)
                if (!enabled) prefs[APP_LOCK_BIOMETRIC_KEY] = false
            }
        }
    }

    fun disableAppLock(currentSecret: String): Boolean {
        if (!hasAppLock || hashSecret(currentSecret) != appLockHash) return false
        return disableAppLockInternal()
    }

    fun disableAppLockWithBiometrics(): Boolean {
        if (!hasAppLock || !appLockBiometricEnabled) return false
        return disableAppLockInternal()
    }

    private fun disableAppLockInternal(): Boolean {
        appLockEnabled = false
        appLockHash = null
        appLockBiometricEnabled = false
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[APP_LOCK_ENABLED_KEY] = false
                prefs.remove(APP_LOCK_HASH_KEY)
                prefs[APP_LOCK_BIOMETRIC_KEY] = false
            }
        }
        return true
    }

    fun tryUnlockApp(secret: String): Boolean {
        val ok = appLockHash != null && hashSecret(secret) == appLockHash
        if (ok) appUnlocked = true
        return ok
    }

    fun unlockAppWithBiometrics() {
        appUnlocked = true
    }

    fun verifyAppLockSecret(secret: String): Boolean =
        appLockHash != null && hashSecret(secret) == appLockHash

    fun changeAppLock(currentSecret: String, newType: LockType, newSecret: String): Boolean {
        if (hasAppLock && hashSecret(currentSecret) != appLockHash) return false
        return changeAppLockInternal(newType, newSecret)
    }

    fun changeAppLockWithBiometrics(newType: LockType, newSecret: String): Boolean {
        if (!hasAppLock || !appLockBiometricEnabled) return false
        return changeAppLockInternal(newType, newSecret)
    }

    private fun changeAppLockInternal(newType: LockType, newSecret: String): Boolean {
        appLockType = newType
        appLockHash = hashSecret(newSecret)
        appLockEnabled = true
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[APP_LOCK_ENABLED_KEY] = true
                prefs[APP_LOCK_TYPE_KEY] = newType.name
                prefs[APP_LOCK_HASH_KEY] = appLockHash!!
            }
        }
        return true
    }

    fun updateAppLockBiometricEnabled(enabled: Boolean) {
        appLockBiometricEnabled = enabled && hasAppLock
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs -> prefs[APP_LOCK_BIOMETRIC_KEY] = appLockBiometricEnabled }
        }
    }

    fun setVaultSecret(type: LockType, secret: String) {
        vaultType = type
        vaultHash = hashSecret(secret)
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[VAULT_TYPE_KEY] = type.name
                prefs[VAULT_HASH_KEY] = vaultHash!!
            }
        }
    }

    fun changeVaultSecret(currentSecret: String, newType: LockType, newSecret: String): Boolean {
        if (vaultConfigured && hashSecret(currentSecret) != vaultHash) return false
        return changeVaultSecretInternal(newType, newSecret)
    }

    fun changeVaultSecretWithBiometrics(newType: LockType, newSecret: String): Boolean {
        if (!vaultConfigured || !vaultBiometricEnabled) return false
        return changeVaultSecretInternal(newType, newSecret)
    }

    private fun changeVaultSecretInternal(newType: LockType, newSecret: String): Boolean {
        vaultType = newType
        vaultHash = hashSecret(newSecret)
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs[VAULT_TYPE_KEY] = newType.name
                prefs[VAULT_HASH_KEY] = vaultHash!!
            }
        }
        return true
    }

    fun removeVault(currentSecret: String): Boolean {
        if (!vaultConfigured || hashSecret(currentSecret) != vaultHash) return false
        return removeVaultInternal()
    }

    fun removeVaultWithBiometrics(): Boolean {
        if (!vaultConfigured || !vaultBiometricEnabled) return false
        return removeVaultInternal()
    }

    private fun removeVaultInternal(): Boolean {
        vaultHash = null
        vaultBiometricEnabled = false
        updateAndPersist(notes.value.map { if (it.locked) it.copy(locked = false) else it })
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs ->
                prefs.remove(VAULT_HASH_KEY)
                prefs[VAULT_BIOMETRIC_KEY] = false
            }
        }
        return true
    }

    fun tryUnlockVault(secret: String): Boolean = vaultHash != null && hashSecret(secret) == vaultHash

    fun unlockVaultWithBiometrics(): Boolean = vaultConfigured

    fun updateVaultBiometricEnabled(enabled: Boolean) {
        vaultBiometricEnabled = enabled && vaultConfigured
        viewModelScope.launch {
            getApplication<Application>().dataStore.edit { prefs -> prefs[VAULT_BIOMETRIC_KEY] = vaultBiometricEnabled }
        }
    }

    /**
     * Builds a full JSON snapshot of everything DataStore holds: notes, folders,
     * appearance/text settings, and lock configuration (hashes + types, not raw secrets).
     * Restoring this snapshot reproduces the exact same state, including PINs/passwords,
     * without ever storing the plaintext secret.
     */
    suspend fun exportBackupJson(): String {
        val prefs = getApplication<Application>().dataStore.data.first()
        val root = JSONObject()
        root.put("backupVersion", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("notes", JSONArray(prefs[NOTES_KEY] ?: "[]"))
        root.put("folders", JSONArray(prefs[FOLDERS_KEY] ?: "[]"))

        val settings = JSONObject()
        settings.put("darkModeEnabled", prefs[DARK_MODE_KEY] ?: true)
        settings.put("dynamicColorEnabled", prefs[DYNAMIC_COLOR_KEY] ?: true)
        settings.put("fontScale", prefs[FONT_SCALE_KEY] ?: 1f)
        settings.put("useSystemFont", prefs[USE_SYSTEM_FONT_KEY] ?: false)
        settings.put("hapticsEnabled", prefs[HAPTICS_ENABLED_KEY] ?: true)
        root.put("settings", settings)

        val lock = JSONObject()
        lock.put("appLockEnabled", prefs[APP_LOCK_ENABLED_KEY] ?: false)
        lock.put("appLockType", prefs[APP_LOCK_TYPE_KEY] ?: LockType.PIN.name)
        if (prefs[APP_LOCK_HASH_KEY] != null) lock.put("appLockHash", prefs[APP_LOCK_HASH_KEY])
        lock.put("vaultType", prefs[VAULT_TYPE_KEY] ?: LockType.PIN.name)
        if (prefs[VAULT_HASH_KEY] != null) lock.put("vaultHash", prefs[VAULT_HASH_KEY])
        root.put("lock", lock)

        return root.toString()
    }

    sealed class RestoreResult {
        data object Success : RestoreResult()
        data class Failure(val message: String) : RestoreResult()
    }

    /**
     * Replaces everything in DataStore with the contents of a previously exported backup,
     * then reloads in-memory state to match. This is destructive to current data by design.
     */
    suspend fun restoreFromBackupJson(json: String): RestoreResult {
        return try {
            val root = JSONObject(json)
            val notesArray = root.optJSONArray("notes") ?: JSONArray()
            val foldersArray = root.optJSONArray("folders") ?: JSONArray()
            val settings = root.optJSONObject("settings") ?: JSONObject()
            val lock = root.optJSONObject("lock") ?: JSONObject()

            getApplication<Application>().dataStore.edit { p ->
                p[NOTES_KEY] = notesArray.toString()
                p[FOLDERS_KEY] = foldersArray.toString()

                p[DARK_MODE_KEY] = settings.optBoolean("darkModeEnabled", true)
                p[DYNAMIC_COLOR_KEY] = settings.optBoolean("dynamicColorEnabled", true)
                p[FONT_SCALE_KEY] = settings.optDouble("fontScale", 1.0).toFloat()
                p[USE_SYSTEM_FONT_KEY] = settings.optBoolean("useSystemFont", false)
                p[HAPTICS_ENABLED_KEY] = settings.optBoolean("hapticsEnabled", true)

                val restoredAppLockEnabled = lock.optBoolean("appLockEnabled", false)
                p[APP_LOCK_ENABLED_KEY] = restoredAppLockEnabled
                p[APP_LOCK_TYPE_KEY] = lock.optString("appLockType", LockType.PIN.name)
                if (lock.has("appLockHash")) p[APP_LOCK_HASH_KEY] = lock.getString("appLockHash")
                else p.remove(APP_LOCK_HASH_KEY)

                p[VAULT_TYPE_KEY] = lock.optString("vaultType", LockType.PIN.name)
                if (lock.has("vaultHash")) p[VAULT_HASH_KEY] = lock.getString("vaultHash")
                else p.remove(VAULT_HASH_KEY)

                p[APP_LOCK_BIOMETRIC_KEY] = false
                p[VAULT_BIOMETRIC_KEY] = false
            }

            // Reload in-memory state from what we just wrote so the UI reflects the restore immediately.
            val prefs = getApplication<Application>().dataStore.data.first()
            darkModeEnabled = prefs[DARK_MODE_KEY] ?: true
            pureDarkMode = prefs[PURE_DARK_KEY] ?: false
            dynamicColorEnabled = prefs[DYNAMIC_COLOR_KEY] ?: true
            fontScale = prefs[FONT_SCALE_KEY] ?: 1f
            useSystemFont = prefs[USE_SYSTEM_FONT_KEY] ?: false
            hapticsEnabled = prefs[HAPTICS_ENABLED_KEY] ?: true
            appLockEnabled = prefs[APP_LOCK_ENABLED_KEY] ?: false
            appLockType = runCatching { LockType.valueOf(prefs[APP_LOCK_TYPE_KEY] ?: LockType.PIN.name) }.getOrDefault(LockType.PIN)
            appLockHash = prefs[APP_LOCK_HASH_KEY]
            vaultHash = prefs[VAULT_HASH_KEY]
            vaultType = runCatching { LockType.valueOf(prefs[VAULT_TYPE_KEY] ?: LockType.PIN.name) }.getOrDefault(LockType.PIN)
            appLockBiometricEnabled = false
            vaultBiometricEnabled = false
            appUnlocked = !hasAppLock
            notes.value = purgeExpiredTrash(loadNotes())
            folders.value = loadFolders()

            RestoreResult.Success
        } catch (_: Exception) {
            RestoreResult.Failure("Invalid or corrupted backup file")
        }
    }

    fun createFolder(name: String, colorTag: String = NoteColor.DEFAULT.name): String {
        val folder = NoteFolder(name = name.trim(), colorTag = colorTag)
        folders.value = listOf(folder) + folders.value
        persistFolders()
        return folder.id
    }

    private fun persistFolders() {
        viewModelScope.launch { saveFolders(folders.value) }
    }

    fun createNote(folderId: String? = null): String {
        val note = Note(title = "", text = "", folderId = folderId)
        updateAndPersist(listOf(note) + notes.value)
        return note.id
    }

    fun trashNote(id: String) {
        updateAndPersist(
            notes.value.map { if (it.id == id) it.copy(trashedAt = System.currentTimeMillis(), archivedAt = null) else it }
        )
    }

    fun restoreNote(id: String) {
        updateAndPersist(notes.value.map { if (it.id == id) it.copy(trashedAt = null) else it })
    }

    fun deleteForever(id: String) {
        updateAndPersist(notes.value.filterNot { it.id == id })
    }

    fun emptyTrash() {
        updateAndPersist(notes.value.filterNot { it.trashedAt != null })
    }

    fun saveNote(id: String, title: String, text: String, colorTag: String, folderId: String?, tags: List<String>) {
        updateAndPersist(
            notes.value.map {
                if (it.id == id) {
                    it.copy(
                        title = title.trim(),
                        text = text.trim(),
                        colorTag = colorTag,
                        folderId = folderId,
                        tags = tags,
                        updatedAt = System.currentTimeMillis()
                    )
                } else it
            }
        )
    }

    fun togglePin(id: String) {
        updateAndPersist(notes.value.map { if (it.id == id) it.copy(pinned = !it.pinned) else it })
    }

    fun toggleFavorite(id: String) {
        updateAndPersist(notes.value.map { if (it.id == id) it.copy(favorite = !it.favorite) else it })
    }

    fun toggleArchive(id: String) {
        updateAndPersist(
            notes.value.map {
                if (it.id == id) it.copy(archivedAt = if (it.archivedAt == null) System.currentTimeMillis() else null)
                else it
            }
        )
    }

    fun toggleLock(id: String) {
        updateAndPersist(notes.value.map { if (it.id == id) it.copy(locked = !it.locked) else it })
    }

    fun noteById(id: String): Note? = notes.value.find { it.id == id }
    fun folderById(id: String?): NoteFolder? = folders.value.find { it.id == id }

    fun allTags(): List<String> =
        notes.value.asSequence().filter { it.trashedAt == null }.flatMap { it.tags }.distinct().sorted().toList()

    fun sortedNotes(): List<Note> =
        notes.value
            .filter { it.trashedAt == null && it.archivedAt == null && !it.locked }
            .sortedWith(compareByDescending<Note> { it.pinned }.thenByDescending { it.updatedAt })

    fun favoriteNotes(): List<Note> =
        sortedNotes().filter { it.favorite }

    fun archivedNotes(): List<Note> =
        notes.value.filter { it.trashedAt == null && it.archivedAt != null && !it.locked }
            .sortedByDescending { it.archivedAt }

    fun vaultNotes(): List<Note> =
        notes.value.filter { it.trashedAt == null && it.locked }
            .sortedByDescending { it.updatedAt }

    fun notesInFolder(folderId: String): List<Note> =
        sortedNotes().filter { it.folderId == folderId }

    fun notesWithTag(tag: String): List<Note> =
        sortedNotes().filter { tag in it.tags }

    fun trashedNotes(): List<Note> =
        notes.value.filter { it.trashedAt != null }.sortedByDescending { it.trashedAt }

    private fun updateAndPersist(newList: List<Note>) {
        notes.value = newList
        viewModelScope.launch { saveNotes(newList) }
    }

    private fun purgeExpiredTrash(list: List<Note>): List<Note> {
        val cutoff = System.currentTimeMillis() - TRASH_RETENTION_MILLIS
        val kept = list.filterNot { it.trashedAt != null && it.trashedAt < cutoff }
        if (kept.size != list.size) {
            viewModelScope.launch { saveNotes(kept) }
        }
        return kept
    }

    private suspend fun loadNotes(): List<Note> {
        return try {
            val prefs = getApplication<Application>().dataStore.data.first()
            val json = prefs[NOTES_KEY] ?: return emptyList()
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                val createdAt = obj.getLong("createdAt")
                val tagsArray = obj.optJSONArray("tags")
                val tags = if (tagsArray != null) (0 until tagsArray.length()).map { tagsArray.getString(it) } else emptyList()
                Note(
                    id = obj.getString("id"),
                    title = obj.optString("title", ""),
                    text = obj.getString("text"),
                    createdAt = createdAt,
                    updatedAt = obj.optLong("updatedAt", createdAt),
                    pinned = obj.optBoolean("pinned", false),
                    favorite = obj.optBoolean("favorite", false),
                    colorTag = obj.optString("colorTag", NoteColor.DEFAULT.name),
                    folderId = if (obj.has("folderId") && !obj.isNull("folderId")) obj.getString("folderId") else null,
                    tags = tags,
                    trashedAt = if (obj.has("trashedAt") && !obj.isNull("trashedAt")) obj.getLong("trashedAt") else null,
                    archivedAt = if (obj.has("archivedAt") && !obj.isNull("archivedAt")) obj.getLong("archivedAt") else null,
                    locked = obj.optBoolean("locked", false)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun saveNotes(noteList: List<Note>) {
        val array = JSONArray()
        noteList.forEach { note ->
            val obj = JSONObject()
            obj.put("id", note.id)
            obj.put("title", note.title)
            obj.put("text", note.text)
            obj.put("createdAt", note.createdAt)
            obj.put("updatedAt", note.updatedAt)
            obj.put("pinned", note.pinned)
            obj.put("favorite", note.favorite)
            obj.put("colorTag", note.colorTag)
            if (note.folderId != null) obj.put("folderId", note.folderId)
            obj.put("tags", JSONArray(note.tags))
            if (note.trashedAt != null) obj.put("trashedAt", note.trashedAt)
            if (note.archivedAt != null) obj.put("archivedAt", note.archivedAt)
            obj.put("locked", note.locked)
            array.put(obj)
        }
        getApplication<Application>().dataStore.edit { it[NOTES_KEY] = array.toString() }
    }

    private suspend fun loadFolders(): List<NoteFolder> {
        return try {
            val prefs = getApplication<Application>().dataStore.data.first()
            val json = prefs[FOLDERS_KEY] ?: return emptyList()
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                NoteFolder(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    colorTag = obj.optString("colorTag", NoteColor.DEFAULT.name),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private suspend fun saveFolders(folderList: List<NoteFolder>) {
        val array = JSONArray()
        folderList.forEach { folder ->
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            obj.put("colorTag", folder.colorTag)
            obj.put("createdAt", folder.createdAt)
            array.put(obj)
        }
        getApplication<Application>().dataStore.edit { it[FOLDERS_KEY] = array.toString() }
    }
}

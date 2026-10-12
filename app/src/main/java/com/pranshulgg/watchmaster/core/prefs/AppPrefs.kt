package com.pranshulgg.watchmaster.core.prefs

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import com.pranshulgg.watchmaster.core.utils.PreferencesHelper
import com.pranshulgg.watchmaster.core.ui.theme.ThemeVariantType
import java.util.UUID

object AppPrefs {
    private val _appTheme = mutableStateOf("System")
    private val _themeColor = mutableStateOf("#2196f3")
    private val _isCustomTheme = mutableStateOf(false)
    private val _useDynamicColor = mutableStateOf(false)

    private val _defaultTab = mutableStateOf("Home")

    private val _themeVariant =
        mutableStateOf(ThemeVariantType.EXPRESSIVE)

    private val _syncFolderUri = mutableStateOf<String?>(null)
    private val _lastSyncTimestamp = mutableStateOf(0L)
    private val _lastWrittenHash = mutableStateOf<String?>(null)
    private val _syncDeviceId = mutableStateOf<String?>(null)

    fun initPrefs(context: Context) {
        PreferencesHelper.init(context)

        _appTheme.value =
            PreferencesHelper.getString("app_theme") ?: "System"
        _themeColor.value = PreferencesHelper.getString("theme_color") ?: "#2196f3"
        _isCustomTheme.value = PreferencesHelper.getBool("isCustomTheme") ?: false
        _useDynamicColor.value = PreferencesHelper.getBool("useDynamicColor") ?: false
        _themeVariant.value =
            PreferencesHelper.getString("theme_variant")
                ?.let {
                    runCatching { ThemeVariantType.valueOf(it) }.getOrNull()
                }
                ?: ThemeVariantType.EXPRESSIVE
        _defaultTab.value = PreferencesHelper.getString("default_tab") ?: "Home"

        _syncFolderUri.value = PreferencesHelper.getString("sync_folder_uri")?.takeIf { it.isNotBlank() }
        _lastSyncTimestamp.value = PreferencesHelper.getString("last_sync_timestamp")?.toLongOrNull() ?: 0L
        _lastWrittenHash.value = PreferencesHelper.getString("last_written_hash")
        _syncDeviceId.value = PreferencesHelper.getString("sync_device_id")
            ?.takeIf { it.isNotBlank() }
            ?: UUID.randomUUID().toString().also {
                PreferencesHelper.setString("sync_device_id", it)
            }
    }

    // Plain (non-Composable) accessors, for use from background/lifecycle
    // code that isn't inside composition — e.g. SyncManager.
    fun getSyncFolderUri(): String? = _syncFolderUri.value

    fun getLastSyncTimestamp(): Long = _lastSyncTimestamp.value

    fun setLastSyncTimestamp(value: Long) {
        _lastSyncTimestamp.value = value
        PreferencesHelper.setString("last_sync_timestamp", value.toString())
    }

    fun getLastWrittenHash(): String? = _lastWrittenHash.value

    fun getSyncDeviceId(): String = _syncDeviceId.value ?: error("AppPrefs not initialized")

    fun setLastWrittenHash(value: String) {
        _lastWrittenHash.value = value
        PreferencesHelper.setString("last_written_hash", value)
    }

    @Composable
    fun state(): AppPrefsState = AppPrefsState(

        appTheme = _appTheme.value,
        setAppTheme = {
            _appTheme.value = it
            PreferencesHelper.setString("app_theme", it)
        },

        themeColor = _themeColor.value,
        setThemeColor = {
            _themeColor.value = it
            PreferencesHelper.setString("theme_color", it)
        },

        isCustomTheme = _isCustomTheme.value,
        useCustomTheme = {
            _isCustomTheme.value = it
            PreferencesHelper.setBool("isCustomTheme", it)
        },

        useDynamicColor = _useDynamicColor.value,
        setDynamicColor = {
            _useDynamicColor.value = it
            PreferencesHelper.setBool("useDynamicColor", it)
        },

        themeVariant = _themeVariant.value,
        setThemeVariant = {
            _themeVariant.value = it
            PreferencesHelper.setString("theme_variant", it.name)
        },

        defaultTab = _defaultTab.value,
        setDefaultTab = {
            _defaultTab.value = it
            PreferencesHelper.setString("default_tab", it)
        },

        syncFolderUri = _syncFolderUri.value,
        setSyncFolderUri = {
            _syncFolderUri.value = it
            PreferencesHelper.setString("sync_folder_uri", it ?: "")
        }
    )
}
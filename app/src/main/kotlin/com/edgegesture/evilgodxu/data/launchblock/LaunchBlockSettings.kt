package com.edgegesture.evilgodxu.data.launchblock

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

val Context.launchBlockDataStore by preferencesDataStore(name = "launch_block")

object LaunchBlockKeys {
    val RULES = stringPreferencesKey("launch_block_rules")
    val ENABLED = booleanPreferencesKey("launch_block_enabled")
}

@Serializable
data class LaunchBlockRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val enabled: Boolean = true,
    val launcherApp: String = "",
    val targetApp: String = "",
    val blockDelay: Int = 0, // 拦截延迟(ms): 0=立即, 500=延时, 1000=延迟
    val enableKillOnFrequentLaunch: Boolean = false,
    val enableKillTarget: Boolean = false,
    val allowKillSystemApp: Boolean = false,
    val launchCount: Int = 0,
    val lastLaunchTime: Long = 0
)

@Serializable
data class LaunchBlockState(
    val rules: List<LaunchBlockRule> = emptyList(),
    val enabled: Boolean = false
)

private val json = Json { ignoreUnknownKeys = true }

fun Context.launchBlockFlow(): Flow<LaunchBlockState> = launchBlockDataStore.data.map { prefs ->
    val rulesJson = prefs[LaunchBlockKeys.RULES] ?: "[]"
    val rules = try {
        json.decodeFromString<List<LaunchBlockRule>>(rulesJson)
    } catch (_: Exception) {
        emptyList()
    }
    LaunchBlockState(
        rules = rules,
        enabled = prefs[LaunchBlockKeys.ENABLED] ?: false
    )
}

suspend fun Context.saveLaunchBlockRules(rules: List<LaunchBlockRule>) = withContext(Dispatchers.IO) {
    launchBlockDataStore.edit { prefs ->
        prefs[LaunchBlockKeys.RULES] = json.encodeToString(rules)
    }
}

suspend fun Context.addLaunchBlockRule(rule: LaunchBlockRule) = withContext(Dispatchers.IO) {
    launchBlockDataStore.edit { prefs ->
        val currentJson = prefs[LaunchBlockKeys.RULES] ?: "[]"
        val currentRules = try {
            json.decodeFromString<List<LaunchBlockRule>>(currentJson)
        } catch (_: Exception) {
            emptyList()
        }
        prefs[LaunchBlockKeys.RULES] = json.encodeToString(currentRules + rule)
    }
}

suspend fun Context.removeLaunchBlockRule(ruleId: String) = withContext(Dispatchers.IO) {
    launchBlockDataStore.edit { prefs ->
        val currentJson = prefs[LaunchBlockKeys.RULES] ?: "[]"
        val currentRules = try {
            json.decodeFromString<List<LaunchBlockRule>>(currentJson)
        } catch (_: Exception) {
            emptyList()
        }
        prefs[LaunchBlockKeys.RULES] = json.encodeToString(
            currentRules.filter { it.id != ruleId }
        )
    }
}

suspend fun Context.updateLaunchBlockRule(updatedRule: LaunchBlockRule) = withContext(Dispatchers.IO) {
    launchBlockDataStore.edit { prefs ->
        val currentJson = prefs[LaunchBlockKeys.RULES] ?: "[]"
        val currentRules = try {
            json.decodeFromString<List<LaunchBlockRule>>(currentJson)
        } catch (_: Exception) {
            emptyList()
        }
        prefs[LaunchBlockKeys.RULES] = json.encodeToString(
            currentRules.map { if (it.id == updatedRule.id) updatedRule else it }
        )
    }
}

suspend fun Context.setLaunchBlockEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
    launchBlockDataStore.edit { prefs ->
        prefs[LaunchBlockKeys.ENABLED] = enabled
    }
}

package com.example.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "anisti_settings")

class PreferencesRepository(private val context: Context) {
    
    companion object {
        val USER_ROLE = stringPreferencesKey("user_role") // "teacher" or "parent"
        val PARENT_CODE = stringPreferencesKey("parent_code")
        val FCM_TOKEN = stringPreferencesKey("fcm_token")
        val TEACHER_PASSWORD = stringPreferencesKey("teacher_password")
        const val DEFAULT_TEACHER_PASSWORD = "123456"
    }

    val userRoleFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[USER_ROLE]
    }

    val parentCodeFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[PARENT_CODE]
    }

    suspend fun getTeacherPassword(): String {
        return context.dataStore.data.map { prefs ->
            prefs[TEACHER_PASSWORD] ?: DEFAULT_TEACHER_PASSWORD
        }.first()
    }

    suspend fun saveTeacherPassword(password: String) {
        context.dataStore.edit { prefs ->
            prefs[TEACHER_PASSWORD] = password
        }
    }

    suspend fun saveUserRole(role: String) {
        context.dataStore.edit { prefs ->
            prefs[USER_ROLE] = role
        }
    }

    suspend fun saveParentCode(code: String) {
        context.dataStore.edit { prefs ->
            prefs[PARENT_CODE] = code
        }
    }

    suspend fun saveFcmToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[FCM_TOKEN] = token
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_ROLE)
            prefs.remove(PARENT_CODE)
            prefs.remove(FCM_TOKEN)
        }
    }
}

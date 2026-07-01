package com.example.repository

class AuthRepository(private val prefsRepository: PreferencesRepository) {

    suspend fun loginTeacher(password: String): Result<Boolean> {
        return try {
            if (password == "123456") {
                prefsRepository.saveUserRole("teacher")
                Result.success(true)
            } else {
                Result.failure(Exception("كلمة السر خاطئة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        prefsRepository.clearSession()
    }
}

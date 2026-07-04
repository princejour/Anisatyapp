package com.example.repository

class AuthRepository(private val prefsRepository: PreferencesRepository) {

    suspend fun loginTeacher(password: String): Result<Boolean> {
        return try {
            if (password == prefsRepository.getTeacherPassword()) {
                prefsRepository.saveUserRole("teacher")
                Result.success(true)
            } else {
                Result.failure(Exception("كلمة السر خاطئة"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun changeTeacherPassword(currentPassword: String, newPassword: String): Result<Boolean> {
        return try {
            if (currentPassword != prefsRepository.getTeacherPassword()) {
                Result.failure(Exception("كلمة السر الحالية خاطئة"))
            } else {
                prefsRepository.saveTeacherPassword(newPassword)
                Result.success(true)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout() {
        prefsRepository.clearSession()
    }
}

package com.example.repository

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()

    suspend fun loginTeacher(password: String): Result<Boolean> {
        return try {
            // Hardcoded teacher email for simplicity, user just enters password "123456"
            // For production, this should be configurable or full login
            auth.signInWithEmailAndPassword("teacher@anisti.com", password).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isTeacherLoggedIn(): Boolean {
        return auth.currentUser != null
    }

    fun logout() {
        auth.signOut()
    }
}

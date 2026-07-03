package com.example.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.repository.FirestoreRepository
import com.example.repository.PreferencesRepository
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentLoginScreen(
    firestoreRepository: FirestoreRepository,
    prefsRepository: PreferencesRepository,
    onLoginSuccess: (String) -> Unit,
    onBack: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دخول الولي") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "مرحباً بك في تطبيق أنيستي حنان",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("أدخل كود التلميذ") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    if (code.isEmpty()) {
                        errorMessage = "الرجاء إدخال الكود"
                        return@Button
                    }
                    isLoading = true
                    errorMessage = null
                    coroutineScope.launch {
                        try {
                            val student = kotlinx.coroutines.withTimeout(10000L) {
                                firestoreRepository.getStudentByCode(code.trim())
                            }

                            if (student != null) {
                                prefsRepository.saveUserRole("parent")
                                prefsRepository.saveParentCode(student.parentCode)

                                isLoading = false
                                onLoginSuccess(student.id)

                                kotlinx.coroutines.GlobalScope.launch {
                                    try {
                                        val token = kotlinx.coroutines.withTimeout(5000L) {
                                            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
                                        }
                                        if (!token.isNullOrEmpty()) {
                                            prefsRepository.saveFcmToken(token)
                                            firestoreRepository.updateParentFcmToken(student.id, token)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                                return@launch
                            } else {
                                errorMessage = "الكود غير صحيح، تأكد من المعلمة."
                            }
                        } catch (e: Exception) {
                            errorMessage = "تعذر فتح حساب الولي. تأكد من الاتصال ثم أعد المحاولة."
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("ربط التطبيق بالتلميذ")
                }
            }
        }
    }
}

package com.example.ui.screens.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.models.SchoolClass
import com.example.repository.FirestoreRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    firestoreRepository: FirestoreRepository,
    onClassClick: (String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val classes by firestoreRepository.getClasses().collectAsState(initial = emptyList())

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val created = firestoreRepository.seedDemoDataIfNeeded()
        if (created) {
            snackbarMessage = "تم إنشاء القوائم التجريبية بنجاح"
            showSnackbar = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة تحكم المعلمة") },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch {
                            val created = firestoreRepository.seedDemoDataIfNeeded()
                            snackbarMessage = if (created) {
                                "تم إنشاء القوائم التجريبية بنجاح"
                            } else {
                                "القوائم التجريبية موجودة مسبقاً"
                            }
                            showSnackbar = true
                        }
                    }) {
                        Text("إنشاء القوائم التجريبية", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onLogout) {
                        Text("خروج", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        snackbarHost = {
            if (showSnackbar) {
                Snackbar(
                    action = {
                        TextButton(onClick = { showSnackbar = false }) {
                            Text("حسناً")
                        }
                    },
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(snackbarMessage)
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (classes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(classes) { schoolClass ->
                        ClassCard(
                            schoolClass = schoolClass,
                            firestoreRepository = firestoreRepository,
                            onClick = { onClassClick(schoolClass.id, schoolClass.name, schoolClass.group) },
                            onDelete = {
                                coroutineScope.launch {
                                    firestoreRepository.deleteClass(schoolClass.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ClassCard(
    schoolClass: SchoolClass,
    firestoreRepository: FirestoreRepository,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val students by firestoreRepository.getStudents(schoolClass.id).collectAsState(initial = emptyList())
    val studentCount = students.size

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = schoolClass.name, style = MaterialTheme.typography.titleLarge)
                Text(text = "عدد التلاميذ: $studentCount", style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف القسم", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

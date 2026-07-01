package com.example.ui.screens.teacher

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        firestoreRepository.initializeDummyData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة تحكم المعلمة") },
                actions = {
                    TextButton(onClick = {
                        coroutineScope.launch { firestoreRepository.initializeDummyData(force = true) }
                    }) {
                        Text("إعادة تهيئة البيانات", color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = onLogout) {
                        Text("خروج", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة قسم")
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

    if (showAddDialog) {
        AddClassDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { level, group ->
                coroutineScope.launch {
                    val name = "$level $group"
                    firestoreRepository.createClass(name, level, group)
                    showAddDialog = false
                }
            }
        )
    }
}

@Composable
fun ClassCard(
    schoolClass: SchoolClass,
    firestoreRepository: FirestoreRepository,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var studentCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(schoolClass.id) {
        studentCount = firestoreRepository.getStudentsCount(schoolClass.id)
    }

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

@Composable
fun AddClassDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var level by remember { mutableStateOf("") }
    var group by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة قسم جديد") },
        text = {
            Column {
                OutlinedTextField(
                    value = level,
                    onValueChange = { level = it },
                    label = { Text("المستوى (مثال: الخامسة)") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = group,
                    onValueChange = { group = it },
                    label = { Text("الفوج (مثال: أ)") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (level.isNotBlank() && group.isNotBlank()) {
                    onAdd(level, group)
                }
            }) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

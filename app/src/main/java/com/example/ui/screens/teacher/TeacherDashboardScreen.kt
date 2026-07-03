package com.example.ui.screens.teacher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.models.SchoolClass
import com.example.models.Student
import com.example.repository.FirestoreRepository
import com.example.util.CsvParser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    firestoreRepository: FirestoreRepository,
    onClassClick: (String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val classes by firestoreRepository.getClasses().collectAsState(initial = emptyList())

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch {
                val importedList = CsvParser.parseStudentImport(context, it)
                val importedNames = importedList.names
                    .map { name -> normalizeDashboardImportedName(name) }
                    .filter { name -> name.isNotBlank() && !isDashboardCorruptedName(name) }
                    .distinct()

                if (importedNames.isEmpty()) {
                    snackbarMessage = "لم يتم العثور على أسماء صالحة في القائمة"
                    showSnackbar = true
                    return@launch
                }

                val db = FirebaseFirestore.getInstance()
                val classSnapshot = db.collection("classes")
                    .whereEqualTo("name", importedList.className)
                    .get()
                    .await()

                val targetClassId = if (!classSnapshot.isEmpty) {
                    classSnapshot.documents.first().id
                } else {
                    val newClass = db.collection("classes").document()
                    newClass.set(
                        mapOf(
                            "id" to newClass.id,
                            "name" to importedList.className,
                            "level" to importedList.level,
                            "group" to importedList.group,
                            "createdAt" to System.currentTimeMillis()
                        )
                    ).await()
                    newClass.id
                }

                val existingSnapshot = db.collection("students")
                    .whereEqualTo("classId", targetClassId)
                    .get()
                    .await()

                val existingNames = existingSnapshot.documents
                    .mapNotNull { doc -> doc.getString("name") }
                    .map { name -> normalizeDashboardImportedName(name) }
                    .toMutableSet()

                val batch = db.batch()
                var addedCount = 0

                importedNames.forEach { name ->
                    if (existingNames.add(name)) {
                        val studentDoc = db.collection("students").document()
                        val student = Student(
                            id = studentDoc.id,
                            name = name,
                            classId = targetClassId,
                            className = importedList.className,
                            parentCode = "Ech-${(1000..9999).random()}"
                        )
                        batch.set(studentDoc, student)
                        addedCount++
                    }
                }

                if (addedCount > 0) {
                    batch.commit().await()
                    snackbarMessage = "تم استيراد $addedCount تلميذ دفعة واحدة"
                } else {
                    snackbarMessage = "القائمة مستوردة سابقاً"
                }
                showSnackbar = true
            }
        }
    }

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
                    TextButton(onClick = { importLauncher.launch("*/*") }) {
                        Text("استيراد قائمة تلاميذ", color = MaterialTheme.colorScheme.primary)
                    }
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

private fun normalizeDashboardImportedName(value: String): String {
    return value.replace(Regex("\\s+"), " ").trim()
}

private fun isDashboardCorruptedName(value: String): Boolean {
    val text = value.trim()
    val lower = text.lowercase()
    if (text.isBlank()) return true
    if (text.contains('�')) return true
    if (text.length > 120) return true
    if (lower.startsWith("pk") || lower.contains("workbook") || lower.contains("xl/") || lower.contains(".xml")) return true
    return false
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

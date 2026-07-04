package com.example.ui.screens.teacher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.models.SchoolClass
import com.example.models.Student
import com.example.repository.AuthRepository
import com.example.repository.FirestoreRepository
import com.example.util.CsvParser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(
    firestoreRepository: FirestoreRepository,
    authRepository: AuthRepository,
    onClassClick: (String, String, String) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val classes by firestoreRepository.getClasses().collectAsState(initial = emptyList())
    val importedClasses = classes.filter { it.source == "import" }
    val visibleClasses = importedClasses.maxByOrNull { it.createdAt }?.let { listOf(it) } ?: emptyList()

    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

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
                val classDoc = db.collection("classes").document()
                val batch = db.batch()

                batch.set(
                    classDoc,
                    mapOf(
                        "id" to classDoc.id,
                        "name" to importedList.className,
                        "level" to importedList.level,
                        "group" to importedList.group,
                        "createdAt" to System.currentTimeMillis(),
                        "source" to "import"
                    )
                )

                val oldCodes = db.collection("students").get().await().documents
                    .mapNotNull { doc ->
                        val name = normalizeDashboardImportedName(doc.getString("name").orEmpty())
                        val code = doc.getString("parentCode").orEmpty()
                        if (name.isNotBlank() && code.isNotBlank()) name to code else null
                    }
                    .toMap()

                importedNames.forEach { name ->
                    val studentDoc = db.collection("students").document()
                    val student = Student(
                        id = studentDoc.id,
                        name = name,
                        classId = classDoc.id,
                        className = importedList.className,
                        parentCode = oldCodes[normalizeDashboardImportedName(name)] ?: "Ech-${(1000..9999).random()}"
                    )
                    batch.set(studentDoc, student)
                }

                batch.commit().await()
                snackbarMessage = "تم استيراد ${importedNames.size} تلميذ"
                showSnackbar = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة تحكم المعلمة") },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "خيارات")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("تغيير كلمة السر") },
                                onClick = {
                                    menuExpanded = false
                                    showChangePasswordDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("استيراد قائمة تلاميذ") },
                                onClick = {
                                    menuExpanded = false
                                    importLauncher.launch("*/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("خروج") },
                                onClick = {
                                    menuExpanded = false
                                    onLogout()
                                }
                            )
                        }
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
            if (visibleClasses.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("استورد قائمة تلاميذ لعرض القسم")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(visibleClasses) { schoolClass ->
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

    if (showChangePasswordDialog) {
        TeacherPasswordChangeDialog(
            authRepository = authRepository,
            onDismiss = { showChangePasswordDialog = false },
            onSuccess = {
                showChangePasswordDialog = false
                snackbarMessage = "تم تغيير كلمة السر بنجاح"
                showSnackbar = true
            }
        )
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

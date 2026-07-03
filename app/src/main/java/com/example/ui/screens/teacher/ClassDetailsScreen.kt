package com.example.ui.screens.teacher

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.models.Student
import com.example.repository.FirestoreRepository
import com.example.ui.navigation.ClassDetailsRoute
import com.example.util.CsvParser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassDetailsScreen(
    route: ClassDetailsRoute,
    firestoreRepository: FirestoreRepository,
    onStudentClick: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val students by firestoreRepository.getStudents(route.classId).collectAsState(initial = emptyList())
    val classes by firestoreRepository.getClasses().collectAsState(initial = emptyList())
    
    var showAddDialog by remember { mutableStateOf(false) }
    var studentToDelete by remember { mutableStateOf<Student?>(null) }
    var studentToEdit by remember { mutableStateOf<Student?>(null) }
    var newName by remember { mutableStateOf("") }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            coroutineScope.launch {
                val importedList = CsvParser.parseStudentImport(context, it, route.className, route.classGroup)
                if (importedList.names.isNotEmpty()) {
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

                    importedList.names.forEach { name ->
                        firestoreRepository.addStudent(name, targetClassId, importedList.className, importedList.group)
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.className) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    TextButton(onClick = { csvLauncher.launch("*/*") }) {
                        Text("استيراد قائمة تلاميذ")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "إضافة تلميذ")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students) { student ->
                    StudentCard(
                        student = student,
                        classes = classes,
                        onClick = { onStudentClick(student.id) },
                        onEdit = { 
                            studentToEdit = student
                            newName = student.name
                        },
                        onMove = { newClassId ->
                            coroutineScope.launch {
                                val targetClass = classes.find { it.id == newClassId }
                                if (targetClass != null) {
                                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    db.collection("students").document(student.id)
                                      .update(
                                          "classId", targetClass.id,
                                          "className", targetClass.name
                                      )
                                }
                            }
                        },
                        onDelete = {
                            studentToDelete = student
                        }
                    )
                }
            }
        }
    }

    if (studentToDelete != null) {
        AlertDialog(
            onDismissRequest = { studentToDelete = null },
            title = { Text("تأكيد الحذف") },
            text = { Text("هل تريد حذف هذا التلميذ؟") },
            confirmButton = {
                Button(onClick = {
                    coroutineScope.launch {
                        firestoreRepository.deleteStudent(studentToDelete!!.id)
                        studentToDelete = null
                    }
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(onClick = { studentToDelete = null }) { Text("إلغاء") }
            }
        )
    }

    if (showAddDialog) {
        AddStudentDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { name ->
                coroutineScope.launch {
                    firestoreRepository.addStudent(name, route.classId, route.className, route.classGroup)
                    showAddDialog = false
                }
            }
        )
    }

    if (studentToEdit != null) {
        AlertDialog(
            onDismissRequest = { studentToEdit = null },
            title = { Text("تعديل التلميذ") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("الاسم") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newName.isNotBlank()) {
                        coroutineScope.launch {
                            firestoreRepository.updateStudent(studentToEdit!!.id, newName)
                            studentToEdit = null
                        }
                    }
                }) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { studentToEdit = null }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun StudentCard(
    student: Student,
    classes: List<com.example.models.SchoolClass>,
    onClick: () -> Unit,
    onEdit: (String) -> Unit,
    onMove: (String) -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showMoveDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = student.name, style = MaterialTheme.typography.titleMedium)
                Text(text = "الكود: ${student.parentCode}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (student.isLinked) "مرتبط بجهاز الولي" else "غير مرتبط",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (student.isLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "خيارات")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text("تعديل الاسم") }, onClick = { menuExpanded = false; onEdit(student.name) })
                    DropdownMenuItem(text = { Text("نقل لقسم آخر") }, onClick = { menuExpanded = false; showMoveDialog = true })
                    DropdownMenuItem(text = { Text("حذف") }, onClick = { menuExpanded = false; onDelete() })
                }
            }
        }
    }

    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("نقل التلميذ") },
            text = {
                Column {
                    Text("اختر القسم الجديد:")
                    Spacer(modifier = Modifier.height(8.dp))
                    classes.forEach { c ->
                        TextButton(onClick = { showMoveDialog = false; onMove(c.id) }, modifier = Modifier.fillMaxWidth()) {
                            Text(c.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun AddStudentDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة تلميذ") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("الاسم واللقب") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onAdd(name) }) { Text("إضافة") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        }
    )
}

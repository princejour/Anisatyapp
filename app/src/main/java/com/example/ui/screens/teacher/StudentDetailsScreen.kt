package com.example.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.models.Message
import com.example.models.Student
import com.example.repository.FirestoreRepository
import kotlinx.coroutines.launch

import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailsScreen(
    studentId: String,
    firestoreRepository: FirestoreRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var student by remember { mutableStateOf<Student?>(null) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ملاحظة", "درس", "تقييم")

    // Form state
    var title by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var grade by remember { mutableStateOf("") }
    var maxGrade by remember { mutableStateOf("20") }
    var importance by remember { mutableStateOf("عادي") }

    var isSending by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(studentId) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val doc = db.collection("students").document(studentId).get().await()
        student = doc.toObject(Student::class.java)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(student?.name ?: "التلميذ") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(16.dp)) {
            if (student != null) {
                Text(text = "القسم: ${student!!.className}")
                Text(text = "الكود: ${student!!.parentCode}", color = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (student!!.isLinked) "الولي متصل" else "الولي غير متصل",
                    color = if (student!!.isLinked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, tabName ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tabName) }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            when (selectedTab) {
                0 -> { // Note
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("العنوان") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("النص") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("درجة الأهمية:")
                    Row {
                        listOf("عادي", "مهم", "عاجل").forEach { opt ->
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                RadioButton(selected = importance == opt, onClick = { importance = opt })
                                Text(opt)
                            }
                        }
                    }
                }
                1 -> { // Lesson
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("المادة") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("عنوان الدرس") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("التفاصيل (واجب منزلي...)") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
                }
                2 -> { // Grade
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("المادة") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Row {
                        OutlinedTextField(value = grade, onValueChange = { grade = it }, label = { Text("العدد") }, modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(value = maxGrade, onValueChange = { maxGrade = it }, label = { Text("على") }, modifier = Modifier.weight(1f))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(value = text, onValueChange = { text = it }, label = { Text("ملاحظة (اختياري)") }, modifier = Modifier.fillMaxWidth())
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    isSending = true
                    successMessage = null
                    coroutineScope.launch {
                        val msgType = when(selectedTab) {
                            0 -> "note"
                            1 -> "lesson"
                            else -> "grade"
                        }
                        val message = Message(
                            studentId = studentId,
                            classId = student?.classId ?: "",
                            type = msgType,
                            title = title,
                            text = text,
                            subject = subject,
                            grade = grade,
                            maxGrade = maxGrade,
                            importance = importance
                        )
                        firestoreRepository.sendMessage(message)
                        isSending = false
                        successMessage = "تم الإرسال بنجاح"
                        title = ""; text = ""; subject = ""; grade = ""
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSending && student != null
            ) {
                Text(if (isSending) "جاري الإرسال..." else "إرسال إلى الولي")
            }
            
            if (successMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(successMessage!!, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

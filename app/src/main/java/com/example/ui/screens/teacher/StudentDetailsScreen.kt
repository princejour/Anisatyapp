package com.example.ui.screens.teacher

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
    val sentMessages by firestoreRepository.getMessagesForStudent(studentId).collectAsState(initial = emptyList())
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
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
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

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "سجل الرسائل المرسلة للولي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (sentMessages.isEmpty()) {
                Text(
                    text = "لم يتم إرسال أي رسالة لهذا الولي بعد",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                sentMessages.forEach { message ->
                    TeacherSentMessageCard(message = message)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun TeacherSentMessageCard(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val typeLabel = when (message.type) {
                    "note" -> "ملاحظة"
                    "lesson" -> "درس"
                    "grade" -> "تقييم"
                    else -> "رسالة"
                }
                Text(text = typeLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (message.importance == "عاجل") {
                    Text(text = "عاجل", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } else if (message.importance == "مهم") {
                    Text(text = "مهم", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }
            if (message.subject.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "المادة: ${message.subject}", style = MaterialTheme.typography.bodyMedium)
            }
            if (message.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            if (message.type == "grade") {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${message.grade} / ${message.maxGrade}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (message.text.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
            }
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

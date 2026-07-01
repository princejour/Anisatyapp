package com.example.ui.screens.parent

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.models.Message
import com.example.models.Student
import com.example.repository.FirestoreRepository
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(
    studentId: String,
    firestoreRepository: FirestoreRepository,
    onLogout: () -> Unit
) {
    var student by remember { mutableStateOf<Student?>(null) }
    val messages by firestoreRepository.getMessagesForStudent(studentId).collectAsState(initial = emptyList())
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("الكل", "الملاحظات", "الدروس", "الأعداد")

    LaunchedEffect(studentId) {
        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val doc = db.collection("students").document(studentId).get().await()
        student = doc.toObject(Student::class.java)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("أنيستي حنان") },
                actions = {
                    TextButton(onClick = onLogout) {
                        Text("خروج", color = MaterialTheme.colorScheme.error)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (student != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = "مرحباً بولي التلميذ: ${student!!.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(text = "القسم: ${student!!.className}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            val filteredMessages = when (selectedTab) {
                1 -> messages.filter { it.type == "note" }
                2 -> messages.filter { it.type == "lesson" }
                3 -> messages.filter { it.type == "grade" }
                else -> messages
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredMessages) { msg ->
                    MessageCard(message = msg)
                }
            }
        }
    }
}

@Composable
fun MessageCard(message: Message) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val typeLabel = when(message.type) {
                    "note" -> "ملاحظة"
                    "lesson" -> "درس"
                    "grade" -> "عدد"
                    else -> "رسالة"
                }
                Text(text = typeLabel, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                if (message.importance == "عاجل") {
                    Text(text = "عاجل", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                } else if (message.importance == "مهم") {
                    Text(text = "مهم", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            if (message.subject.isNotEmpty()) {
                Text(text = "المادة: ${message.subject}", style = MaterialTheme.typography.bodyMedium)
            }
            
            Text(text = message.title, style = MaterialTheme.typography.titleMedium)
            
            if (message.type == "grade") {
                Text(
                    text = "${message.grade} / ${message.maxGrade}",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            if (message.text.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = message.text, style = MaterialTheme.typography.bodyMedium)
            }
            
            // Format timestamp (simplified)
            val date = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date(message.timestamp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

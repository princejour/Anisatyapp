package com.example.repository

import com.example.models.Message
import com.example.models.SchoolClass
import com.example.models.Student
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()

    // --- Classes ---
    fun getClasses(): Flow<List<SchoolClass>> = callbackFlow {
        val subscription = db.collection("classes")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val classes = snapshot.toObjects(SchoolClass::class.java)
                    trySend(classes)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun createClass(name: String, level: String, group: String) {
        val doc = db.collection("classes").document()
        val schoolClass = SchoolClass(
            id = doc.id,
            name = name,
            level = level,
            group = group
        )
        doc.set(schoolClass).await()
    }

    suspend fun deleteClass(classId: String) {
        db.collection("classes").document(classId).delete().await()
    }
    
    suspend fun getStudentsCount(classId: String): Int {
        val snapshot = db.collection("students").whereEqualTo("classId", classId).get().await()
        return snapshot.size()
    }

    // --- Students ---
    fun getStudents(classId: String): Flow<List<Student>> = callbackFlow {
        val subscription = db.collection("students")
            .whereEqualTo("classId", classId)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val students = snapshot.toObjects(Student::class.java)
                    trySend(students)
                }
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addStudent(name: String, classId: String, className: String, group: String, customCode: String? = null) {
        val doc = db.collection("students").document()
        
        val parentCode = customCode ?: run {
            val randomSuffix = (1000..9999).random()
            "Ech-$randomSuffix"
        }
        
        val student = Student(
            id = doc.id,
            name = name,
            classId = classId,
            className = className,
            parentCode = parentCode
        )
        doc.set(student).await()
    }

    suspend fun updateStudent(studentId: String, newName: String) {
        db.collection("students").document(studentId).update("name", newName).await()
    }

    suspend fun deleteStudent(studentId: String) {
        db.collection("students").document(studentId).delete().await()
    }

    // --- Parent side ---
    suspend fun linkParent(code: String, deviceToken: String): Result<Student> {
        return try {
            val snapshot = db.collection("students")
                .whereEqualTo("parentCode", code)
                .get()
                .await()
                
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                val student = doc.toObject(Student::class.java)!!
                
                // Update link status
                db.collection("students").document(student.id)
                    .update(
                        "isLinked", true,
                        "parentDeviceId", deviceToken
                    ).await()
                    
                Result.success(student)
            } else {
                Result.failure(Exception("كود غير صحيح"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStudentByCode(code: String): Student? {
        val snapshot = db.collection("students")
            .whereEqualTo("parentCode", code)
            .get()
            .await()
        return if (!snapshot.isEmpty) {
            snapshot.documents.first().toObject(Student::class.java)
        } else null
    }

    // --- Messages ---
    suspend fun sendMessage(message: Message) {
        val doc = db.collection("messages").document()
        val msgWithId = message.copy(id = doc.id)
        doc.set(msgWithId).await()
    }

    fun getMessagesForStudent(studentId: String): Flow<List<Message>> = callbackFlow {
        val subscription = db.collection("messages")
            .whereEqualTo("studentId", studentId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val msgs = snapshot.toObjects(Message::class.java)
                    trySend(msgs)
                }
            }
        awaitClose { subscription.remove() }
    }

    // --- Demo Data Init ---
    suspend fun seedDemoDataIfNeeded(): Boolean {
        val classesSnap = db.collection("classes").get().await()
        val studentsSnap = db.collection("students").get().await()
        
        val has5A = classesSnap.documents.any { it.getString("name") == "الخامسة أ" }
        val has5B = classesSnap.documents.any { it.getString("name") == "الخامسة ب" }
        
        val students5ACount = studentsSnap.documents.count { it.getString("className") == "الخامسة أ" }
        val students5BCount = studentsSnap.documents.count { it.getString("className") == "الخامسة ب" }
        
        // If everything is perfectly set up, return false
        if (has5A && has5B && students5ACount >= 22 && students5BCount >= 22 && classesSnap.size() == 2) {
            return false
        }
        
        // Otherwise, clean up everything (duplicates, empties, etc.) to start fresh
        for (doc in studentsSnap.documents) { doc.reference.delete().await() }
        for (doc in classesSnap.documents) { doc.reference.delete().await() }

        val class5A = db.collection("classes").document()
        class5A.set(SchoolClass(class5A.id, "الخامسة أ", "الخامسة", "أ")).await()

        val class5B = db.collection("classes").document()
        class5B.set(SchoolClass(class5B.id, "الخامسة ب", "الخامسة", "ب")).await()

        val students5A = listOf(
            "أحمد بن علي", "مريم الجبالي", "ياسين التومي", "إيناس العلوي", "سيف الدين مرزوق",
            "نور الدين السالمي", "آية بن يوسف", "محمد أمين التريكي", "رانيا العياري", "سارة بن محمود",
            "آدم الرقيق", "ملاك الزواري", "إلياس الكعبي", "جنى الفقيه", "يوسف الهمامي",
            "سلمى بن صالح", "كريم الجلاصي", "هدى الماجري", "ريان الغربي", "لينا الشابي",
            "فراس بن عمر", "تقوى العبيدي"
        )
        students5A.forEachIndexed { index, name ->
            val numStr = (index + 1).toString().padStart(3, '0')
            val code = "HANAN-5A-$numStr"
            addStudent(name, class5A.id, "الخامسة أ", "5A", code)
        }

        val students5B = listOf(
            "حنان السالمي", "آدم الرقيق", "سارة العياري", "مالك بن يوسف", "نور الهدى الكعبي",
            "سيف العابدين بن علي", "آمنة الجبالي", "مروان التومي", "إسراء العلوي", "يوسف مرزوق",
            "رقية السعدي", "أنس بن رمضان", "لميس الطرابلسي", "مهدي بن سالم", "مريم القاسمي",
            "أيوب الفقيه", "سيرين الغربي", "حمزة بن محمود", "ندى الزواري", "وليد الشابي",
            "جودي الهمامي", "خليل العبيدي"
        )
        students5B.forEachIndexed { index, name ->
            val numStr = (index + 1).toString().padStart(3, '0')
            val code = "HANAN-5B-$numStr"
            addStudent(name, class5B.id, "الخامسة ب", "5B", code)
        }
        
        return true // Data was created
    }
}

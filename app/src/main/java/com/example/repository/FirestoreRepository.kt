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
        
        val classesByName = classesSnap.documents.groupBy { it.getString("name") }
        
        var class5AId: String? = null
        var class5BId: String? = null
        
        var modificationsMade = false

        // Process "الخامسة أ"
        val class5AList = classesByName["الخامسة أ"]
        if (!class5AList.isNullOrEmpty()) {
            class5AId = class5AList.first().id
            for (i in 1 until class5AList.size) {
                db.collection("classes").document(class5AList[i].id).delete().await()
                val duplicateStudents = db.collection("students").whereEqualTo("classId", class5AList[i].id).get().await()
                for (doc in duplicateStudents) doc.reference.delete().await()
                modificationsMade = true
            }
        } else {
            val newClass = db.collection("classes").document()
            newClass.set(SchoolClass(newClass.id, "الخامسة أ", "الخامسة", "أ")).await()
            class5AId = newClass.id
            modificationsMade = true
        }

        // Process "الخامسة ب"
        val class5BList = classesByName["الخامسة ب"]
        if (!class5BList.isNullOrEmpty()) {
            class5BId = class5BList.first().id
            for (i in 1 until class5BList.size) {
                db.collection("classes").document(class5BList[i].id).delete().await()
                val duplicateStudents = db.collection("students").whereEqualTo("classId", class5BList[i].id).get().await()
                for (doc in duplicateStudents) doc.reference.delete().await()
                modificationsMade = true
            }
        } else {
            val newClass = db.collection("classes").document()
            newClass.set(SchoolClass(newClass.id, "الخامسة ب", "الخامسة", "ب")).await()
            class5BId = newClass.id
            modificationsMade = true
        }

        // Handle students for 5A
        val students5ASnap = db.collection("students").whereEqualTo("classId", class5AId).get().await()
        val currentStudents5A = students5ASnap.documents.mapNotNull { it.getString("name") }.toSet()
        
        val targetStudents5A = listOf(
            "أحمد بن علي", "مريم الجبالي", "ياسين التومي", "إيناس العلوي", "سيف الدين مرزوق",
            "نور الدين السالمي", "آية بن يوسف", "محمد أمين التريكي", "رانيا العياري", "سارة بن محمود",
            "آدم الرقيق", "ملاك الزواري", "إلياس الكعبي", "جنى الفقيه", "يوسف الهمامي",
            "سلمى بن صالح", "كريم الجلاصي", "هدى الماجري", "ريان الغربي", "لينا الشابي",
            "فراس بن عمر", "تقوى العبيدي"
        )
        
        targetStudents5A.forEachIndexed { index, name ->
            if (!currentStudents5A.contains(name)) {
                val numStr = (index + 1).toString().padStart(3, '0')
                val code = "HANAN-5A-$numStr"
                addStudent(name, class5AId!!, "الخامسة أ", "5A", code)
                modificationsMade = true
            }
        }

        // Handle students for 5B
        val students5BSnap = db.collection("students").whereEqualTo("classId", class5BId).get().await()
        val currentStudents5B = students5BSnap.documents.mapNotNull { it.getString("name") }.toSet()

        val targetStudents5B = listOf(
            "حنان السالمي", "آدم الرقيق", "سارة العياري", "مالك بن يوسف", "نور الهدى الكعبي",
            "سيف العابدين بن علي", "آمنة الجبالي", "مروان التومي", "إسراء العلوي", "يوسف مرزوق",
            "رقية السعدي", "أنس بن رمضان", "لميس الطرابلسي", "مهدي بن سالم", "مريم القاسمي",
            "أيوب الفقيه", "سيرين الغربي", "حمزة بن محمود", "ندى الزواري", "وليد الشابي",
            "جودي الهمامي", "خليل العبيدي"
        )
        
        targetStudents5B.forEachIndexed { index, name ->
            if (!currentStudents5B.contains(name)) {
                val numStr = (index + 1).toString().padStart(3, '0')
                val code = "HANAN-5B-$numStr"
                addStudent(name, class5BId!!, "الخامسة ب", "5B", code)
                modificationsMade = true
            }
        }
        
        return modificationsMade
    }
}

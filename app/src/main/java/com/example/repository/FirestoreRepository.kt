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

    suspend fun addStudent(name: String, classId: String, className: String, group: String) {
        val doc = db.collection("students").document()
        val randomSuffix = (1000..9999).random()
        // Example: Ech-1234
        val parentCode = "Ech-$randomSuffix"
        
        val student = Student(
            id = doc.id,
            name = name,
            classId = classId,
            className = className,
            parentCode = parentCode
        )
        doc.set(student).await()
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

    // --- Dummy Data Init ---
    suspend fun initializeDummyData(force: Boolean = false) {
        val studentsSnap = db.collection("students").get().await()
        if (force || studentsSnap.size() < 40) {
            // Delete existing classes and students to start fresh
            val classesSnap = db.collection("classes").get().await()
            for (doc in classesSnap.documents) { doc.reference.delete().await() }
            for (doc in studentsSnap.documents) { doc.reference.delete().await() }

            val class5A = db.collection("classes").document()
            class5A.set(SchoolClass(class5A.id, "الخامسة أ", "الخامسة", "أ")).await()

            val class5B = db.collection("classes").document()
            class5B.set(SchoolClass(class5B.id, "الخامسة ب", "الخامسة", "ب")).await()

            val students5A = listOf(
                "أحمد بن علي", "مريم الجبالي", "ياسين التومي", "إيناس العلوي", "سيف الدين مرزوق",
                "محمد العياري", "فاطمة الزهراء", "عمر الفاروق", "يوسف الشاهد", "نور الهدى",
                "علي بن سالم", "سلمى العبيدي", "خليل الطرابلسي", "سارة الماجري", "ريان المبروك",
                "آدم الغربي", "لينا الخياري", "مهدي الفالح", "آية بن عمار", "رامي السويسي",
                "ميساء الجريدي", "إلياس البجاوي"
            )
            students5A.forEach { name ->
                addStudent(name, class5A.id, "الخامسة أ", "5A")
            }

            val students5B = listOf(
                "حنان السالمي", "آدم الرقيق", "سارة العياري", "مالك بن يوسف", "نور الهدى الكعبي",
                "ياسر الفتني", "ريتاج المولهي", "أنس الرياحي", "مروان الحداد", "شهد الغرياني",
                "زينب القروي", "محمد أمين", "فراس الشابي", "مرام الباهي", "عزيز الجوادي",
                "جنى الزغلامي", "يحيى العوني", "أريج الميساوي", "حاتم الدرويش", "شروق المكي",
                "سامي العابد", "إسراء الدالي"
            )
            students5B.forEach { name ->
                addStudent(name, class5B.id, "الخامسة ب", "5B")
            }
        }
    }
}

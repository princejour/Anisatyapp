package com.example.models

import java.util.Date

data class SchoolClass(
    val id: String = "",
    val name: String = "",
    val level: String = "",
    val group: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Student(
    val id: String = "",
    val name: String = "",
    val classId: String = "",
    val className: String = "",
    val parentCode: String = "",
    val isLinked: Boolean = false,
    val parentDeviceId: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class Message(
    val id: String = "",
    val studentId: String = "",
    val classId: String = "",
    val type: String = "", // "note", "lesson", "grade"
    val title: String = "",
    val text: String = "",
    val subject: String = "",
    val grade: String = "",
    val maxGrade: String = "",
    val importance: String = "عادي", // "عادي", "مهم", "عاجل"
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

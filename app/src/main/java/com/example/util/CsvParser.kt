package com.example.util

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.InputStreamReader

object CsvParser {
    fun parseStudentsFromCsv(context: Context, uri: Uri): List<String> {
        val students = mutableListOf<String>()
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val reader = BufferedReader(InputStreamReader(inputStream))
            
            var line: String? = reader.readLine()
            // Skip header if it exists
            if (line != null && (line.contains("الاسم", ignoreCase = true) || line.contains("name", ignoreCase = true))) {
                line = reader.readLine()
            }
            
            while (line != null) {
                if (line.isNotBlank()) {
                    // Assuming comma separated, taking the first column
                    val name = line.split(",")[0].trim()
                    if (name.isNotEmpty()) {
                        students.add(name)
                    }
                }
                line = reader.readLine()
            }
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return students
    }
}

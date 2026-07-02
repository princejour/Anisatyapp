package com.example.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale

object EmbeddedImageData {
    const val HERO_TEACHER = "UklGRs4BAABXRUJQVlA4IMIBAAAQDACdASpgAEgAPzmWk06kpKiqtIaEFmAbiWYAagAAe3Y/N/437fSsHFuFvWKAeQu+2CKnF9ejujF2cqEiU70ZceqVH5uK68RoBGbdJwiyZfwV2XIrKGvYAAP73SVu0ThFdq0J3+03cLsxwHps8aIMasV6Md2CTb52eZhD5Z1WVRy8yYFcbt2HLeG5D0sLW0Yk1kAGHd7X1o/oqPpC1fpdl4S1VtiSSx6AgvZVNf5qcbUbn8O4YjAjMfX7gImyaODycwSRL88uEAJPF2S3oEJkZ7+2ajtdt87Yw41j9k2wNrm5QfWOLdSb4RqS7drzjqQzQxmfFvEPhoC2Z40Jkg7MCz4Q/uT7hhJWWkxSvt5Q9+bqraHoYzQU8DcHZSqFQzz17bbnB+wCq5VZQvHrjHfuqTifGmVXXVdYhb0bqsK7nX3IxANrCeqwSZH8Q3J8rJ/sLr/e4pEgjFUmeJ4fH9Rzj4QwuD7bXAC3Ut8II3qOAzth7jh3tqy9+7nBA8ctHWipGVncWeNZCRpJdTfHfaOTUD5oXh8oInqxSbOy5Z+hJmClN/S7JqnsOh66B1rca9dXLJWV8dkM0aYb0OxCEtQxQSMhv9Vd9o02EHtdKSOQ9QRC6o8UFMf4J4cVFVWm14i2KQ73FqYSmVKNyWa4NbeHMt3qxCRhaqxEbLJHVrS3gMd4oG6BVL9ljqT39f+4d1kPms8sZzKpz/dfez+6X3OrzqlW9t+hX0gAA"
    const val TEACHER_ICON = "UklGRoYBAABXRUJQVlA4IHoBAAAQDACdASpIAEgAPzmQkUQkIqIhJbAEFmAbiWcAagBSvi5FPL5jeQ/S+1jL0G3cn3clLLsrAo1Rxq7vcv3nvJiw/1hc4x+G+vxj+2t6cJRzYnOMx/ZtA2RAp1J4FOVZ08kg5PQU7g0oHh9W3g7is3ZAgA/vcjRmVRb1H6kN5nJGT8uFPsG0hE5+sKz2+62F8K2jBEggbBQ8A43jY+u90GBIg9re7iXP5wld7lMcd+3AUH0NXj4u0WJcm/C0SS9x6tEjgMqmA3E3Kh1aVeLEEY4HpQli2YHrbXza2dJ/yRqe6tLMLG53qj8ztnanGu+am7VijYAx0eeLFTuI9V8eRQpg/BUcmEYO0HQTqXjfCsrWWjZWlhrc8HKieaIu25iKufPLbP1xYxBpCKH7kro81/vEtDKt8g6NfqFxMdEU9FjGM8kK3HUkLyrPZvbcaFV7CjAkLxcyF3xd+IwI8GdzfuSDIhp9Y6D384rW+emv2GSU2l/6FExvcwzkfslBDqJKv/rCKFTqhXJFXbDYmSzmrte7qv1ggAA"
    const val PARENTS_ICON = "UklGRrQAAABXRUJQVlA4IKgAAACwCACdASpIAEgAPzmYiESkIqEhpqAEFiWkAlwADeajHe6+/yct+WPcHNm0PK1XXD9E+NRE33eHAA=="
}

@Composable
fun EmbeddedImage(encoded: String, contentDescription: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val imageBitmap = remember(encoded) {
        try {
            val bytes = Base64.decode(encoded, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Exception) { null }
    }
    if (imageBitmap != null) {
        Image(bitmap = imageBitmap, contentDescription = contentDescription, contentScale = contentScale, modifier = modifier)
    } else {
        Icon(imageVector = Icons.Filled.Face, contentDescription = contentDescription, modifier = modifier)
    }
}

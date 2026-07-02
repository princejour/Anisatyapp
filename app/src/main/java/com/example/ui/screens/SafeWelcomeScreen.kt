package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun SafeWelcomeScreen(onTeacherClick: () -> Unit, onParentClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFBFF))
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(54.dp))
        Image(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(24.dp))
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("أنيستي حنان", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1C1B1F))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "التواصل الذكي والآمن بين المعلمة وأولياء التلاميذ",
            fontSize = 16.sp,
            color = Color(0xFF49454F),
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )
        Spacer(modifier = Modifier.height(38.dp))
        Column(
            modifier = Modifier
                .size(280.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFF3EDF7), RoundedCornerShape(40.dp)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.teacher_illustration),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
            )
            Text(
                "مرحباً بك في تطبيقك الخاص.\nيرجى اختيار نوع الدخول للمتابعة.",
                fontSize = 14.sp,
                color = Color(0xFF79747E),
                textAlign = TextAlign.Center,
                lineHeight = 24.sp,
                modifier = Modifier.padding(16.dp)
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        EntryRow("دخول المعلمة", true, R.drawable.teacher_badge, onTeacherClick)
        Spacer(modifier = Modifier.height(12.dp))
        EntryRow("دخول الولي", false, R.drawable.parents_badge, onParentClick)
        Spacer(modifier = Modifier.height(16.dp))
        Text("جميع الحقوق محفوظة للمعلمة حنان © 2024", fontSize = 10.sp, color = Color(0xFF938F99), fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun EntryRow(title: String, filled: Boolean, imageRes: Int, action: () -> Unit) {
    val bg = if (filled) Color(0xFF6750A4) else Color.White
    val fg = if (filled) Color.White else Color(0xFF1C1B1F)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(2.dp, if (filled) Color.Transparent else Color(0xFFCAC4D0), RoundedCornerShape(16.dp))
            .clickable { action() }
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

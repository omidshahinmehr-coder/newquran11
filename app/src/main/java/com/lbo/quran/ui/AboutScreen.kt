package com.lbo.quran.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lbo.quran.R

private const val DEVELOPER_NAME = "امید شاهین‌مهر"
private const val DEVELOPER_EMAIL = "omidshahinmehr@gmail.com"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("درباره برنامه") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = "نماد برنامه",
                modifier = Modifier.size(140.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "قرآن کریم",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "جستجو، ترجمه استاد انصاریان و تفسیر البرهان فی تفسیر القرآن",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))
            HorizontalDivider()
            Spacer(Modifier.height(24.dp))

            Text("طراحی و برنامه‌نویسی", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                DEVELOPER_NAME,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(16.dp))
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = Uri.parse("mailto:$DEVELOPER_EMAIL")
                }
                context.startActivity(intent)
            }) {
                Icon(Icons.Default.Email, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(DEVELOPER_EMAIL)
            }
        }
    }
}

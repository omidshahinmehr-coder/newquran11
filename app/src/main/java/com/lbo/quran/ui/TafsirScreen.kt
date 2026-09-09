package com.lbo.quran.ui

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.ui.theme.quranFontByKey
import com.lbo.quran.ui.theme.translationFontByKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirScreen(
    viewModel: QuranViewModel,
    aId: String,
    surahName: String,
    ayahNumber: Int,
    onBack: () -> Unit
) {
    val state by viewModel.tafsir.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(aId) {
        viewModel.loadTafsir(aId, surahName, ayahNumber)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفسیر البرهان — $surahName، آیه $ayahNumber") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (state.language == "fa") 1 else 0) {
                Tab(
                    selected = state.language == "ar",
                    onClick = { viewModel.setTafsirLanguage("ar") },
                    text = { Text("عربی") }
                )
                Tab(
                    selected = state.language == "fa",
                    onClick = { viewModel.setTafsirLanguage("fa") },
                    text = { Text("فارسی") }
                )
            }

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (state.entries.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("تفسیری برای این آیه ثبت نشده است.")
                }
                return@Column
            }

            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                state.ayah?.let { ayah ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            SelectionContainer {
                                Text(
                                    "${ayah.text}  (${ayah.ayahNumber})",
                                    fontFamily = quranFontByKey(settings.quranFontKey),
                                    fontSize = (settings.quranFontSize * 0.85f).sp,
                                    lineHeight = (settings.quranFontSize * 1.6).sp,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                }

                items(state.entries) { entry ->
                    val sourceLabel = if (entry.language == "fa") "ترجمه تفسیر البرهان" else "تفسیر البرهان"
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(settings.tafsirBackgroundColor))
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(
                                                Intent.EXTRA_TEXT,
                                                "${entry.text}\n\n$sourceLabel — $surahName، آیه $ayahNumber"
                                            )
                                        }
                                        context.startActivity(Intent.createChooser(intent, null))
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = "اشتراک‌گذاری",
                                        tint = Color(settings.tafsirTextColor),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            SelectionContainer {
                                Text(
                                    entry.text,
                                    fontFamily = translationFontByKey(settings.translationFontKey),
                                    fontSize = settings.translationFontSize.sp,
                                    lineHeight = (settings.translationFontSize * 1.6).sp,
                                    color = Color(settings.tafsirTextColor)
                                )
                            }
                        }
                    }
                }

                if (state.footnotes.isNotEmpty()) {
                    item {
                        Text(
                            "پاورقی‌ها",
                            style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(state.footnotes) { note ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            SelectionContainer {
                                Text(
                                    note.text,
                                    fontFamily = translationFontByKey(settings.translationFontKey),
                                    fontSize = (settings.translationFontSize * 0.9f).sp,
                                    lineHeight = (settings.translationFontSize * 1.5).sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

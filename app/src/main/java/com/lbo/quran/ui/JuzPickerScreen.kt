package com.lbo.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JuzPickerScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onJuzSelected: (Int) -> Unit
) {
    val state by viewModel.juzList.collectAsState()

    LaunchedEffect(Unit) {
        if (state.juzList.isEmpty()) viewModel.loadJuzList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("فهرست اجزاء") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        if (state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(state.juzList) { juz ->
                ListItem(
                    headlineContent = { Text("جزء ${juz.juzNumber}") },
                    supportingContent = { Text("شروع از ${juz.startSurahName}، آیه ${juz.startAyahNumber}") },
                    leadingContent = {
                        Box(modifier = Modifier.size(32.dp), contentAlignment = Alignment.Center) {
                            Text(juz.juzNumber.toString(), style = MaterialTheme.typography.labelLarge)
                        }
                    },
                    modifier = Modifier.clickable { onJuzSelected(juz.juzNumber) }
                )
                HorizontalDivider()
            }
        }
    }
}

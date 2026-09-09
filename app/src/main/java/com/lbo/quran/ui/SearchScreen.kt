package com.lbo.quran.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onOpenAyah: (String) -> Unit,
    onOpenTafsirEntry: (Int, String, Long) -> Unit
) {
    val state by viewModel.search.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadSearchHistory()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جستجو") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { viewModel.updateQuery(it) },
                label = { Text("جستجو در قرآن و تفسیر البرهان") },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = { viewModel.runSearch() }) {
                        Icon(Icons.Default.Search, contentDescription = "جستجو")
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.runSearch() }),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            Row {
                FilterChip(
                    selected = state.includeQuran,
                    onClick = { viewModel.toggleFilter("quran"); viewModel.runSearch() },
                    label = { Text("قرآن") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.includeTafsirAr,
                    onClick = { viewModel.toggleFilter("tafsirAr"); viewModel.runSearch() },
                    label = { Text("تفسیر البرهان") }
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = state.includeTafsirFa,
                    onClick = { viewModel.toggleFilter("tafsirFa"); viewModel.runSearch() },
                    label = { Text("ترجمه تفسیر البرهان") }
                )
            }

            Spacer(Modifier.height(12.dp))

            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }

            if (state.query.isBlank() && state.history.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("جستجوهای اخیر", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { viewModel.clearSearchHistory() }) {
                        Text("پاک کردن")
                    }
                }
                LazyColumn {
                    items(state.history) { q ->
                        ListItem(
                            headlineContent = { Text(q) },
                            leadingContent = { Icon(Icons.Default.History, contentDescription = null) },
                            modifier = Modifier.clickable { viewModel.useHistoryQuery(q) }
                        )
                        HorizontalDivider()
                    }
                }
                return@Column
            }

            LazyColumn {
                items(state.results) { result ->
                    val terms = extractSearchTerms(state.query)
                    val truncated = smartTruncateAroundMatch(result.snippet, terms, isArabic = true)
                    val highlighted = highlightArabic(truncated, terms)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = {
                            if (result.kind == "quran") {
                                onOpenAyah(result.aId)
                            } else {
                                val language = if (result.kind == "tafsir_fa") "fa" else "ar"
                                result.tafsirId?.let { onOpenTafsirEntry(result.surahNumber, language, it) }
                            }
                        }
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(
                                "${result.surahNameFa} - آیه ${result.ayahNumber}  [" +
                                    kindLabel(result.kind) + "]",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(highlighted, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

private fun kindLabel(kind: String) = when (kind) {
    "quran" -> "متن قرآن"
    "tafsir_ar" -> "تفسیر البرهان"
    "tafsir_fa" -> "ترجمه تفسیر البرهان"
    else -> kind
}

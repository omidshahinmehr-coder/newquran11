package com.lbo.quran.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.ui.theme.translationFontByKey
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TafsirBrowseScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit,
    onOpenSearch: () -> Unit
) {
    val state by viewModel.tafsirBrowse.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val surahListState by viewModel.surahList.collectAsState()
    val scrollTargetId by viewModel.tafsirBrowseScrollTarget.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (scrollTargetId == null) {
            if (state.entriesAr.isNotEmpty() || state.entriesFa.isNotEmpty()) {
                // قبلاً در همین نشست بارگذاری شده (مثلاً برگشتیم از صفحه‌ی دیگر)؛
                // به همان پاراگراف قبلی برمی‌گردیم، نه ابتدای لیست.
                viewModel.getTafsirSessionScrollId()?.let { viewModel.setTafsirBrowseScrollTarget(it) }
            } else {
                // اولین ورود در طول عمر برنامه: یا آخرین محل ذخیره‌شده (از نشست قبلی برنامه) را
                // بازیابی کن یا کل کتاب را طبق فیلتر فعلی بارگذاری کن.
                val resume = viewModel.consumeInitialScrollTafsir()
                if (resume != null) {
                    val (language, tafsirId) = resume
                    viewModel.openTafsirEntry(surahNumber = null, language = language, tafsirId = tafsirId)
                } else {
                    viewModel.loadTafsirBrowse(state.surahFilter)
                }
            }
        }
        if (surahListState.surahs.isEmpty()) viewModel.loadSurahList()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفسیر البرهان") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Default.Search, contentDescription = "جستجو")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = if (state.language == "fa") 1 else 0) {
                Tab(
                    selected = state.language == "ar",
                    onClick = { viewModel.setTafsirBrowseLanguage("ar") },
                    text = { Text("عربی") }
                )
                Tab(
                    selected = state.language == "fa",
                    onClick = { viewModel.setTafsirBrowseLanguage("fa") },
                    text = { Text("فارسی") }
                )
            }

            SurahFilterDropdown(
                surahs = surahListState.surahs,
                selectedSurah = state.surahFilter,
                onSelect = { surahNumber -> viewModel.loadTafsirBrowse(surahNumber) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            Spacer(Modifier.height(8.dp))

            if (state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val results = state.entries
            if (results.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("تفسیری یافت نشد.")
                }
                return@Column
            }

            LaunchedEffect(scrollTargetId, results) {
                val targetId = scrollTargetId ?: return@LaunchedEffect
                val index = results.indexOfFirst { it.id == targetId }
                if (index >= 0) {
                    listState.scrollToItem(index)
                    // اصلاح دقیق موقعیت برای پاراگراف‌های بلند تفسیر (همان دلیل صفحه‌ی اصلی)
                    listState.scrollToItem(index)
                    viewModel.consumeTafsirBrowseScrollTarget()
                }
            }

            // ذخیره‌ی خودکار آخرین پاراگراف در حال مطالعه (مثل قرآن)
            LaunchedEffect(results, state.language) {
                snapshotFlow { listState.firstVisibleItemIndex }
                    .distinctUntilChanged()
                    .debounce(1000)
                    .collect { index ->
                        results.getOrNull(index)?.let { viewModel.saveLastReadTafsir(state.language, it.id) }
                    }
            }

            SelectionContainer {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(results) { entry ->
                        val bookmarkKey = "${state.language}:${entry.id}"
                        val isBookmarked = bookmarkKey in state.bookmarkedTafsirIds
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (entry.id == scrollTargetId)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    Color(settings.tafsirBackgroundColor)
                            )
                        ) {
                            Column {
                                Text(
                                    entry.text,
                                    fontFamily = translationFontByKey(settings.translationFontKey),
                                    fontSize = settings.translationFontSize.sp,
                                    lineHeight = (settings.translationFontSize * 1.6).sp,
                                    color = Color(settings.tafsirTextColor),
                                    modifier = Modifier.padding(16.dp)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(end = 4.dp, bottom = 4.dp),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    IconButton(
                                        onClick = { viewModel.toggleTafsirBookmark(state.language, entry.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = if (isBookmarked) "حذف نشانک" else "نشانک‌گذاری",
                                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurahFilterDropdown(
    surahs: List<com.lbo.quran.data.SurahInfo>,
    selectedSurah: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = surahs.firstOrNull { it.surahNumber == selectedSurah }?.nameFa ?: "کل کتاب"

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("فیلتر سوره") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("کل کتاب") },
                onClick = { onSelect(null); expanded = false }
            )
            surahs.forEach { surah ->
                DropdownMenuItem(
                    text = { Text("${surah.surahNumber}. ${surah.nameFa}") },
                    onClick = { onSelect(surah.surahNumber); expanded = false }
                )
            }
        }
    }
}

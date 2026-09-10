package com.lbo.quran.ui

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.data.ReadingItem
import com.lbo.quran.data.WordEntity
import com.lbo.quran.ui.theme.quranFontByKey
import com.lbo.quran.ui.theme.resolveFontStyle
import com.lbo.quran.ui.theme.resolveFontWeight
import com.lbo.quran.ui.theme.translationFontByKey
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private val WAQF_MARK_COLOR = Color(0xFF8B0000)   // زرشکی برای علائم وقف/سکته/سجده/حزب
private val BISMILLAH_COLOR = Color(0xFFB8860B)   // رنگ متفاوت (طلایی تیره) برای بسم‌الله
private const val FATIHA_AYAH1_ID = "001001"       // آیه اول فاتحه؛ بسم‌الله همان چهار کلمه اول این آیه است
private const val WORD_TAG = "word"

// فونت طاها به‌خودی‌خود قلمی نازک/کالیگرافیک است و روی برخی صفحه‌نمایش‌ها کم‌رنگ دیده می‌شود.
// به‌جای دستکاری خودِ فایل فونت (که ریسک بهم‌ریختن اشکال حروف را دارد)، یک لایه‌ی نازک از
// همان متن با drawStyle=Stroke زیر متن اصلی کشیده می‌شود تا ضخامت حروف کمی بیشتر به نظر برسد؛
// این افکت فقط وقتی فونت انتخابی «طاها» باشد فعال است.
private val TAHA_STROKE_BOOST_DP = 0.35f

/** ساخت متن حاشیه‌دار (رنگی) آیه از روی کلمات جدول Words_taha؛ همچنین هر کلمه‌ی
 *  عادی/بسم‌الله را با یک annotation قابل‌لمس مشخص می‌کند تا بشود روی آن لمس کرد
 *  و معنی‌اش را دید.
 *  bismillahPrefixCount: تعداد کلمه‌ی ابتدایی که باید هم‌رنگ بسم‌الله شوند
 *  (فقط برای آیه اول فاتحه کاربرد دارد، چون آنجا بسم‌الله بخشی از خود آیه است) */
private fun buildWordsAnnotatedString(
    words: List<WordEntity>,
    textColor: Color,
    bismillahPrefixCount: Int = 0,
    quranFontKey: String = "taha"
): AnnotatedString =
    buildAnnotatedString {
        words.forEachIndexed { index, w ->
            if (index > 0) append(" ")
            val display = if (w.type == 3) {
                if (quranFontKey == "taha") {
                    // فونت طاها برای این کاراکتر (پایان‌آیه قرآنی) طرح دایره‌ای اختصاصی خودش را دارد
                  //  "${w.text}\u06DD"
                   "(${w.text})" 
                } else {
                    "(${w.text})"
                }
            } else w.text
            val color = when {
                index < bismillahPrefixCount -> BISMILLAH_COLOR
                w.type == 0 || w.type == 4 || w.type == 5 || w.type == 7 -> WAQF_MARK_COLOR
                else -> textColor
            }
            val start = length
            withStyle(SpanStyle(color = color)) { append(display) }
            if (w.type == 3) {
                // فاصله‌ی خالی بعد از نشان تزئینی پایان‌آیه؛ چون این کاراکتر معمولاً
                // آخرین جزء متن است و فضای خالی بعدش وجود ندارد، موتور چیدمان متن
                // نمی‌داند کجا سطر را بشکند و ممکن است از لبه‌ی کادر بیرون بزند.
                // این فاصله یک نقطه‌ی شکست معتبر ایجاد می‌کند.
                append(" ")
            }
            if (w.type == 1 || w.type == 6) {
                addStringAnnotation(tag = WORD_TAG, annotation = index.toString(), start = start, end = length)
            }
        }
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: QuranViewModel,
    onOpenSurahPicker: () -> Unit,
    onOpenJuzPicker: () -> Unit,
    onOpenHizbPicker: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenTafsirBrowse: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAudioSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenTafsir: (aId: String, surahName: String, ayahNumber: Int) -> Unit
) {
    val state by viewModel.fullQuran.collectAsState()
    val scrollTarget by viewModel.scrollTarget.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val playback by viewModel.audioController.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showHint by remember { mutableStateOf(true) }
    var showPlaybackMenu by remember { mutableStateOf(false) }
    var showRepeatMenu by remember { mutableStateOf(false) }
    val repeatCount by viewModel.audioRepeatCount.collectAsState()
    var showPageDialog by remember { mutableStateOf(false) }
    var pageInput by remember { mutableStateOf("") }
    var pageError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadFullQuran()
    }

    // بازگشت از صفحه تفسیر: اسکرول به همان آیه‌ای که تفسیرش باز شده بود
    // یا (فقط بار اول در طول عمر برنامه) اسکرول به آخرین محل مطالعه ذخیره‌شده
    LaunchedEffect(state.items.size) {
        if (state.items.isNotEmpty()) {
            val returnAyah = viewModel.consumePendingReturnAyah()
            val target = returnAyah ?: viewModel.consumeInitialScrollAyah()
            target?.let { aId ->
                viewModel.itemIndexForAyah(aId)?.let { index ->
                    listState.scrollToItem(index)
                    // اصلاح دقیق موقعیت برای آیات بلند (نگاه کنید به توضیح پایین‌تر)
                    listState.scrollToItem(index)
                }
            }
        }
    }

    // ذخیره خودکار آخرین آیه‌ای که کاربر در حال مشاهده آن است (با تأخیر کوتاه، برای جلوگیری از نوشتن مکرر)
    LaunchedEffect(state.items) {
        if (state.items.isEmpty()) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .debounce(1000)
            .collect { index ->
                val visibleAyah = state.items.drop(index)
                    .firstOrNull { it is ReadingItem.Ayah } as? ReadingItem.Ayah
                visibleAyah?.let { viewModel.saveLastReadPosition(it.ayah.aId) }
            }
    }

    // انتخاب سوره/جزء از منو، یا بازگشت از نتیجه‌ی جستجو/نشانک، حتی وقتی صفحه اصلی از قبل باز است
    LaunchedEffect(scrollTarget) {
        scrollTarget?.let { index ->
            listState.scrollToItem(index)
            // اصلاح دقیق موقعیت: LazyColumn برای آیتم‌هایی که هنوز اندازه‌گیری نشده‌اند از یک
            // میانگین تخمینی استفاده می‌کند؛ چون آیات طول خیلی متفاوتی دارند، این تخمین می‌تواند
            // نادقیق باشد. فراخوانی دوم، بعد از اینکه آیتم هدف واقعاً اندازه‌گیری شد، موقعیت را
            // دقیقاً روی ابتدای همان آیتم تنظیم می‌کند.
            listState.scrollToItem(index)
            viewModel.consumeScrollTarget()
        }
    }

    // هم‌زمان با پخش صوت، به‌صورت خودکار روی آیه‌ی در حال پخش اسکرول کن
    LaunchedEffect(playback.currentAId) {
        val aId = playback.currentAId ?: return@LaunchedEffect
        viewModel.itemIndexForAyah(aId)?.let { index ->
            listState.animateScrollToItem(index)
            // اصلاح دقیق موقعیت: در آیات بلند، تخمین ارتفاع در حین انیمیشن ممکن است نادقیق باشد
            // و اسکرول وسط آیه متوقف شود؛ این اسکرول فوری، ابتدای دقیق آیتم را تضمین می‌کند.
            listState.scrollToItem(index)
        }
    }

    // اطلاعات آیه‌ای که هم‌اکنون بالای صفحه قرار دارد (نام سوره، جزء، حزب، صفحه)؛ با اسکرول به‌روزرسانی می‌شود
    val currentAyahItem by remember {
        derivedStateOf {
            val items = state.items
            if (items.isEmpty()) return@derivedStateOf null
            val start = listState.firstVisibleItemIndex.coerceIn(0, items.size - 1)
            var idx = start
            while (idx < items.size) {
                val current = items[idx]
                if (current is ReadingItem.Ayah) return@derivedStateOf current
                idx++
            }
            idx = start
            while (idx >= 0) {
                val current = items[idx]
                if (current is ReadingItem.Ayah) return@derivedStateOf current
                idx--
            }
            null
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(12.dp))
                Text(
                    "قرآن کریم",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("فهرست سوره‌ها") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenSurahPicker() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("فهرست اجزاء") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenJuzPicker() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("نشانک‌های من") },
                    icon = { Icon(Icons.Default.Bookmarks, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenBookmarks() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    "زبان ترجمه",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    FilterChip(
                        selected = settings.translationLanguage == "fa",
                        onClick = { viewModel.updateSettings(settings.copy(translationLanguage = "fa")) },
                        label = { Text("فارسی") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    FilterChip(
                        selected = settings.translationLanguage == "en",
                        onClick = { viewModel.updateSettings(settings.copy(translationLanguage = "en")) },
                        label = { Text("English") }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                NavigationDrawerItem(
                    label = { Text("تفسیر البرهان") },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenTafsirBrowse() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("تنظیمات نمایش") },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenSettings() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("صدای تلاوت") },
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenAudioSettings() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
                NavigationDrawerItem(
                    label = { Text("درباره برنامه") },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    selected = false,
                    onClick = { scope.launch { drawerState.close() }; onOpenAbout() },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("قرآن کریم") },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "منو")
                            }
                        },
                        actions = {
                            Text(
                                "ترجمه",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            Switch(
                                checked = state.showTranslation,
                                onCheckedChange = { viewModel.toggleFullQuranTranslationVisible() }
                            )
                            Box {
                                IconButton(onClick = { showPlaybackMenu = true }) {
                                    Icon(Icons.Default.PlaylistPlay, contentDescription = "پخش صوت")
                                }
                                DropdownMenu(expanded = showPlaybackMenu, onDismissRequest = { showPlaybackMenu = false }) {
                                    DropdownMenuItem(
                                        text = { Text("پخش این آیه") },
                                        onClick = {
                                            showPlaybackMenu = false
                                            currentAyahItem?.let { viewModel.playAyah(it.ayah.aId) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("پخش این صفحه") },
                                        onClick = {
                                            showPlaybackMenu = false
                                            currentAyahItem?.let { viewModel.playPage(it.ayah.page, it.ayah.aId) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("پخش این سوره") },
                                        onClick = {
                                            showPlaybackMenu = false
                                            currentAyahItem?.let { viewModel.playSurah(it.ayah.surahNumber, it.ayah.aId) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("پخش این حزب") },
                                        onClick = {
                                            showPlaybackMenu = false
                                            currentAyahItem?.let { viewModel.playHizb(it.ayah.hizb, it.ayah.aId) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("پخش این جزء") },
                                        onClick = {
                                            showPlaybackMenu = false
                                            currentAyahItem?.let { viewModel.playJuz(it.ayah.juz, it.ayah.aId) }
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("پخش کل قرآن از اینجا") },
                                        onClick = {
                                            showPlaybackMenu = false
                                            currentAyahItem?.let { viewModel.playWholeQuran(it.ayah.aId) }
                                        }
                                    )
                                }
                            }
                            IconButton(onClick = onOpenSearch) {
                                Icon(Icons.Default.Search, contentDescription = "جستجو")
                            }
                        }
                    )
                    currentAyahItem?.let { meta ->
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "جزء ${meta.ayah.juz}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f).clickable { onOpenJuzPicker() },
                                    textAlign = TextAlign.Start
                                )
                                Text(
                                    meta.surahNameFa,
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(2f).clickable { onOpenSurahPicker() }
                                )
                                Text(
                                    "حزب ${meta.ayah.hizb}",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.weight(1f).clickable { onOpenHizbPicker() },
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            },
            bottomBar = {
                Column {
                    if (playback.isActive) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(playback.scopeLabel, style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            "آیه ${playback.currentIndex + 1} از ${playback.queue.size}",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Box {
                                        TextButton(onClick = { showRepeatMenu = true }) {
                                            Text("تکرار: $repeatCount")
                                        }
                                        DropdownMenu(expanded = showRepeatMenu, onDismissRequest = { showRepeatMenu = false }) {
                                            (1..5).forEach { n ->
                                                DropdownMenuItem(
                                                    text = { Text(n.toString()) },
                                                    onClick = {
                                                        viewModel.setAudioRepeatCount(n)
                                                        showRepeatMenu = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { viewModel.audioController.previous() }) {
                                        Icon(Icons.Default.SkipPrevious, contentDescription = "قبلی")
                                    }
                                    IconButton(onClick = { viewModel.audioController.togglePlayPause() }) {
                                        Icon(
                                            if (playback.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                            contentDescription = if (playback.isPlaying) "توقف" else "پخش"
                                        )
                                    }
                                    IconButton(onClick = { viewModel.audioController.next() }) {
                                        Icon(Icons.Default.SkipNext, contentDescription = "بعدی")
                                    }
                                    IconButton(onClick = { viewModel.audioController.stop() }) {
                                        Icon(Icons.Default.Close, contentDescription = "بستن پخش")
                                    }
                                }
                            }
                        }
                    }
                    currentAyahItem?.let { meta ->
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer) {
                            Text(
                                "صفحه ${meta.ayah.page}",
                                style = MaterialTheme.typography.labelMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .clickable {
                                        pageInput = meta.ayah.page.toString()
                                        pageError = null
                                        showPageDialog = true
                                    }
                            )
                        }
                    }
                }
            }
        ) { padding ->
            if (state.loading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Scaffold
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
            ) {
                if (showHint) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "برای مشاهده تفسیر البرهان یا اشتراک‌گذاری، از آیکون‌های زیر هر آیه استفاده کنید. برای رفتن به سوره یا جزء دیگر از منو استفاده کنید.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { showHint = false }) {
                                    Text("×", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }

                items(state.items) { item ->
                    when (item) {
                        is ReadingItem.SurahHeader -> {
                            Text(
                                item.surahNameFa,
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp, bottom = 4.dp)
                            )
                        }
                        is ReadingItem.Bismillah -> {
                            Text(
                                buildWordsAnnotatedString(item.words, BISMILLAH_COLOR, quranFontKey = settings.quranFontKey),
                                fontFamily = quranFontByKey(settings.quranFontKey),
                                fontSize = settings.quranFontSize.sp,
                                fontWeight = resolveFontWeight(settings.quranFontWeight) ?: FontWeight.Medium,
                                fontStyle = resolveFontStyle(settings.quranFontItalic),
                                letterSpacing = settings.quranLetterSpacing.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                        }
                        is ReadingItem.Ayah -> {
                            val ayah = item.ayah
                            val surahNameFa = item.surahNameFa
                            val hasTafsir = ayah.aId in state.ayahIdsWithTafsir
                            val context = LocalContext.current
                            var selectedWord by remember(ayah.aId) { mutableStateOf<WordEntity?>(null) }
                            var textLayout by remember(ayah.aId) { mutableStateOf<TextLayoutResult?>(null) }
                            val annotatedAyahText = buildWordsAnnotatedString(
                                item.words,
                                Color(settings.quranTextColor),
                                bismillahPrefixCount = if (ayah.aId == FATIHA_AYAH1_ID) 4 else 0,
                                quranFontKey = settings.quranFontKey
                            )

                            selectedWord?.let { word ->
                                AlertDialog(
                                    onDismissRequest = { selectedWord = null },
                                    title = { Text(word.text, style = MaterialTheme.typography.titleLarge) },
                                    text = {
                                        Column {
                                            val meaning = word.meaningFa.ifBlank { word.meaningAr }
                                            Text(meaning.ifBlank { "معنی این کلمه هنوز ثبت نشده است." })
                                            if (word.composition.isNotBlank()) {
                                                Spacer(Modifier.height(12.dp))
                                                Text("ترکیب:", style = MaterialTheme.typography.labelMedium)
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    word.composition,
                                                    style = MaterialTheme.typography.bodyLarge
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        TextButton(onClick = { selectedWord = null }) { Text("بستن") }
                                    }
                                )
                            }

                            val isPlayingThisAyah = ayah.aId == playback.currentAId
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isPlayingThisAyah)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        Color(settings.quranBackgroundColor)
                                )
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    SelectionContainer {
                                        Column {
                                            Box(modifier = Modifier.fillMaxWidth()) {
                                                if (settings.quranFontKey == "taha") {
                                                    // لایه‌ی تقویت‌کننده‌ی ضخامت (فقط تزئینی، غیرقابل‌انتخاب/لمس)
                                                    val strokePx = with(LocalDensity.current) { TAHA_STROKE_BOOST_DP.dp.toPx() }
                                                    DisableSelection {
                                                        Text(
                                                            annotatedAyahText,
                                                            fontFamily = quranFontByKey(settings.quranFontKey),
                                                            fontSize = settings.quranFontSize.sp,
                                                            fontWeight = resolveFontWeight(settings.quranFontWeight),
                                                            fontStyle = resolveFontStyle(settings.quranFontItalic),
                                                            letterSpacing = settings.quranLetterSpacing.sp,
                                                            lineHeight = (settings.quranFontSize * settings.quranLineHeightMultiplier).sp,
                                                            textAlign = TextAlign.Right,
                                                            style = LocalTextStyle.current.copy(drawStyle = Stroke(width = strokePx)),
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .padding(end = 6.dp)
                                                        )
                                                    }
                                                }
                                                Text(
                                                    annotatedAyahText,
                                                    fontFamily = quranFontByKey(settings.quranFontKey),
                                                    fontSize = settings.quranFontSize.sp,
                                                    fontWeight = resolveFontWeight(settings.quranFontWeight),
                                                    fontStyle = resolveFontStyle(settings.quranFontItalic),
                                                    letterSpacing = settings.quranLetterSpacing.sp,
                                                    lineHeight = (settings.quranFontSize * settings.quranLineHeightMultiplier).sp,
                                                    textAlign = TextAlign.Right,
                                                    onTextLayout = { textLayout = it },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(end = 6.dp)
                                                        .pointerInput(item.words) {
                                                            detectTapGestures { offset ->
                                                                val layout = textLayout ?: return@detectTapGestures
                                                                val charIndex = layout.getOffsetForPosition(offset)
                                                                annotatedAyahText
                                                                    .getStringAnnotations(WORD_TAG, charIndex, charIndex)
                                                                    .firstOrNull()
                                                                    ?.item
                                                                    ?.toIntOrNull()
                                                                    ?.let { idx -> selectedWord = item.words.getOrNull(idx) }
                                                            }
                                                        }
                                                )
                                            }
                                            if (state.showTranslation) {
                                                state.translations[ayah.aId]?.let { tr ->
                                                    Spacer(Modifier.height(8.dp))
                                                    val isEnglish = settings.translationLanguage == "en"
                                                    Text(
                                                        tr.text,
                                                        fontFamily = if (isEnglish) null else translationFontByKey(settings.translationFontKey),
                                                        fontSize = settings.translationFontSize.sp,
                                                        fontWeight = resolveFontWeight(settings.translationFontWeight),
                                                        fontStyle = resolveFontStyle(settings.translationFontItalic),
                                                        letterSpacing = settings.translationLetterSpacing.sp,
                                                        lineHeight = (settings.translationFontSize * settings.translationLineHeightMultiplier).sp,
                                                        color = Color(settings.quranTextColor),
                                                        textAlign = if (isEnglish) TextAlign.Left else TextAlign.Justify,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val isBookmarked = ayah.aId in state.bookmarkedAyahIds
                                        IconButton(
                                            onClick = { viewModel.playAyah(ayah.aId) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.PlayCircle,
                                                contentDescription = "پخش این آیه",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.toggleBookmark(ayah.aId) },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                                contentDescription = if (isBookmarked) "حذف نشانک" else "نشانک‌گذاری",
                                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        if (hasTafsir) {
                                            IconButton(
                                                onClick = {
                                                    viewModel.rememberReturnAyah(ayah.aId)
                                                    onOpenTafsir(ayah.aId, surahNameFa, ayah.ayahNumber)
                                                },
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.MenuBook,
                                                    contentDescription = "نمایش تفسیر",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = {
                                                val shareBody = buildString {
                                                    append(ayah.text)
                                                    if (state.showTranslation) {
                                                        state.translations[ayah.aId]?.let {
                                                            append("\n\n")
                                                            append(it.text)
                                                        }
                                                    }
                                                    append("\n\n")
                                                    append("$surahNameFa — ${ayah.ayahNumber}")
                                                }
                                                val intent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "text/plain"
                                                    putExtra(Intent.EXTRA_TEXT, shareBody)
                                                }
                                                context.startActivity(Intent.createChooser(intent, null))
                                            },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.Share,
                                                contentDescription = "اشتراک‌گذاری آیه",
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

    if (showPageDialog) {
        AlertDialog(
            onDismissRequest = { showPageDialog = false },
            title = { Text("رفتن به صفحه") },
            text = {
                Column {
                    Text("شماره صفحه را بین ${state.minPage} تا ${state.maxPage} وارد کنید.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = {
                            pageInput = it.filter { ch -> ch.isDigit() }
                            pageError = null
                        },
                        singleLine = true,
                        isError = pageError != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    pageError?.let { err ->
                        Spacer(Modifier.height(4.dp))
                        Text(err, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val page = pageInput.toIntOrNull()
                    when {
                        page == null -> pageError = "شماره صفحه معتبر نیست."
                        !viewModel.requestScrollToPage(page) ->
                            pageError = "شماره صفحه باید بین ${state.minPage} و ${state.maxPage} باشد."
                        else -> showPageDialog = false
                    }
                }) { Text("برو") }
            },
            dismissButton = {
                TextButton(onClick = { showPageDialog = false }) { Text("انصراف") }
            }
        )
    }
}

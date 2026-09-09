package com.lbo.quran.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lbo.quran.ui.theme.FontWeightOptions
import com.lbo.quran.ui.theme.QuranFontOptions
import com.lbo.quran.ui.theme.TranslationFontOptions
import com.lbo.quran.ui.theme.quranFontByKey
import com.lbo.quran.ui.theme.resolveFontStyle
import com.lbo.quran.ui.theme.resolveFontWeight
import com.lbo.quran.ui.theme.translationFontByKey

private val TEXT_COLOR_SWATCHES = listOf(
    0xFF1B1B1B, 0xFFFFFFFF, 0xFF5B3A29, 0xFF7A1F1F,
    0xFF1F4B2F, 0xFF1F3A5F, 0xFF5A1F5A, 0xFFB8860B
)

private val BACKGROUND_COLOR_SWATCHES = listOf(
    0xFFFFFFFF, 0xFFFBF3E0, 0xFFF3EFE6, 0xFFEFF6EE,
    0xFFEAF1F8, 0xFF1B1B1B, 0xFF262626, 0xFF0F2A1C
)

/** ترکیب‌های آماده و سنجیده‌ی رنگ برای تجربه‌ی خواندن راحت‌تر؛ هر گزینه هم رنگ متن/پس‌زمینه‌ی
 *  قرآن و هم تفسیر را هم‌زمان تنظیم می‌کند تا کاربر مجبور به انتخاب دستی دو رنگ هماهنگ نباشد. */
private data class ReadingTheme(
    val label: String,
    val textColor: Long,
    val backgroundColor: Long
)

private val READING_THEMES = listOf(
    ReadingTheme("روز", 0xFF1B1B1B, 0xFFFFFFFF),
    ReadingTheme("سپیا", 0xFF5B3A29, 0xFFFBF3E0),
    ReadingTheme("شب", 0xFFFFFFFF, 0xFF1B1B1B)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیمات نمایش") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("پیش‌فرض‌های آماده‌ی خواندن", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                "با یک لمس، رنگ متن و پس‌زمینه‌ی قرآن و تفسیر را هم‌زمان به یک ترکیب هماهنگ تغییر دهید.",
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                READING_THEMES.forEach { theme ->
                    val textArgb = theme.textColor.toInt()
                    val bgArgb = theme.backgroundColor.toInt()
                    val isSelected = settings.quranTextColor == textArgb &&
                        settings.quranBackgroundColor == bgArgb &&
                        settings.tafsirTextColor == textArgb &&
                        settings.tafsirBackgroundColor == bgArgb
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                viewModel.updateSettings(
                                    settings.copy(
                                        quranTextColor = textArgb,
                                        quranBackgroundColor = bgArgb,
                                        tafsirTextColor = textArgb,
                                        tafsirBackgroundColor = bgArgb
                                    )
                                )
                            }
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFBBBBBB),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(Color(bgArgb))
                            .padding(vertical = 10.dp, horizontal = 14.dp)
                    ) {
                        Text("ابج", color = Color(textArgb), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(theme.label, color = Color(textArgb), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text("متن قرآن", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            FontDropdown(
                options = QuranFontOptions,
                selectedKey = settings.quranFontKey,
                onSelect = { viewModel.updateSettings(settings.copy(quranFontKey = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Text("اندازه قلم: ${settings.quranFontSize.toInt()}")
            Slider(
                value = settings.quranFontSize,
                onValueChange = { viewModel.updateSettings(settings.copy(quranFontSize = it)) },
                valueRange = 16f..40f,
                steps = 11
            )

            Spacer(Modifier.height(12.dp))
            FontWeightDropdown(
                label = "وزن فونت قرآن",
                selectedRaw = settings.quranFontWeight,
                onSelect = { viewModel.updateSettings(settings.copy(quranFontWeight = it)) }
            )
            Text(
                "توجه: چون فایل فونت طاها فقط در یک وزن ساخته شده، تغییر این گزینه ممکن است روی آن اثر چندانی نداشته باشد؛ برای فونت‌های دیگر معمولاً قابل مشاهده است.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("حالت کج (ایتالیک)", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.quranFontItalic,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(quranFontItalic = it)) }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("فاصله‌ی حروف: ${"%.1f".format(settings.quranLetterSpacing)}")
            Slider(
                value = settings.quranLetterSpacing,
                onValueChange = { viewModel.updateSettings(settings.copy(quranLetterSpacing = it)) },
                valueRange = 0f..3f,
                steps = 29
            )
            Text(
                "توصیه می‌شود این مقدار برای متن قرآن روی صفر بماند؛ زیادکردنش ممکن است اتصال حروف عربی را بهم بزند.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Text("فاصله‌ی خطوط: ${"%.1f".format(settings.quranLineHeightMultiplier)} برابر")
            Slider(
                value = settings.quranLineHeightMultiplier,
                onValueChange = { viewModel.updateSettings(settings.copy(quranLineHeightMultiplier = it)) },
                valueRange = 1.2f..3f,
                steps = 17
            )

            Spacer(Modifier.height(12.dp))
            Text("رنگ متن", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ColorSwatchRow(
                swatches = TEXT_COLOR_SWATCHES,
                selected = settings.quranTextColor,
                onSelect = { viewModel.updateSettings(settings.copy(quranTextColor = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Text("رنگ پس‌زمینه", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ColorSwatchRow(
                swatches = BACKGROUND_COLOR_SWATCHES,
                selected = settings.quranBackgroundColor,
                onSelect = { viewModel.updateSettings(settings.copy(quranBackgroundColor = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(settings.quranBackgroundColor))) {
                Text(
                    "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                    fontFamily = quranFontByKey(settings.quranFontKey),
                    fontSize = settings.quranFontSize.sp,
                    fontWeight = resolveFontWeight(settings.quranFontWeight),
                    fontStyle = resolveFontStyle(settings.quranFontItalic),
                    letterSpacing = settings.quranLetterSpacing.sp,
                    lineHeight = (settings.quranFontSize * settings.quranLineHeightMultiplier).sp,
                    color = Color(settings.quranTextColor),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            Text("متن ترجمه و تفسیر", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            FontDropdown(
                options = TranslationFontOptions,
                selectedKey = settings.translationFontKey,
                onSelect = { viewModel.updateSettings(settings.copy(translationFontKey = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Text("اندازه قلم: ${settings.translationFontSize.toInt()}")
            Slider(
                value = settings.translationFontSize,
                onValueChange = { viewModel.updateSettings(settings.copy(translationFontSize = it)) },
                valueRange = 12f..28f,
                steps = 15
            )

            Spacer(Modifier.height(12.dp))
            FontWeightDropdown(
                label = "وزن فونت ترجمه/تفسیر",
                selectedRaw = settings.translationFontWeight,
                onSelect = { viewModel.updateSettings(settings.copy(translationFontWeight = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("حالت کج (ایتالیک)", modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.translationFontItalic,
                    onCheckedChange = { viewModel.updateSettings(settings.copy(translationFontItalic = it)) }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("فاصله‌ی حروف: ${"%.1f".format(settings.translationLetterSpacing)}")
            Slider(
                value = settings.translationLetterSpacing,
                onValueChange = { viewModel.updateSettings(settings.copy(translationLetterSpacing = it)) },
                valueRange = 0f..3f,
                steps = 29
            )

            Spacer(Modifier.height(12.dp))
            Text("فاصله‌ی خطوط: ${"%.1f".format(settings.translationLineHeightMultiplier)} برابر")
            Slider(
                value = settings.translationLineHeightMultiplier,
                onValueChange = { viewModel.updateSettings(settings.copy(translationLineHeightMultiplier = it)) },
                valueRange = 1.1f..2.5f,
                steps = 13
            )

            Spacer(Modifier.height(12.dp))
            Text("رنگ متن تفسیر", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ColorSwatchRow(
                swatches = TEXT_COLOR_SWATCHES,
                selected = settings.tafsirTextColor,
                onSelect = { viewModel.updateSettings(settings.copy(tafsirTextColor = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Text("رنگ پس‌زمینه تفسیر", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            ColorSwatchRow(
                swatches = BACKGROUND_COLOR_SWATCHES,
                selected = settings.tafsirBackgroundColor,
                onSelect = { viewModel.updateSettings(settings.copy(tafsirBackgroundColor = it)) }
            )

            Spacer(Modifier.height(12.dp))
            Card(colors = CardDefaults.cardColors(containerColor = Color(settings.tafsirBackgroundColor))) {
                Text(
                    "این متن نمونه‌ای از رنگ و فونت انتخابی برای ترجمه و تفسیر است.",
                    fontFamily = translationFontByKey(settings.translationFontKey),
                    fontSize = settings.translationFontSize.sp,
                    fontWeight = resolveFontWeight(settings.translationFontWeight),
                    fontStyle = resolveFontStyle(settings.translationFontItalic),
                    letterSpacing = settings.translationLetterSpacing.sp,
                    lineHeight = (settings.translationFontSize * settings.translationLineHeightMultiplier).sp,
                    color = Color(settings.tafsirTextColor),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ColorSwatchRow(
    swatches: List<Long>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        swatches.forEach { swatchLong ->
            val argb = swatchLong.toInt()
            val isSelected = argb == selected
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(argb))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFBBBBBB),
                        shape = CircleShape
                    )
                    .clickable { onSelect(argb) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    val iconTint = if (Color(argb).luminance() > 0.5f) Color.Black else Color.White
                    Icon(Icons.Default.Check, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontWeightDropdown(
    label: String,
    selectedRaw: Int,
    onSelect: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = FontWeightOptions.firstOrNull { it.raw == selectedRaw }?.label ?: FontWeightOptions.first().label

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            FontWeightOptions.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        onSelect(option.raw)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FontDropdown(
    options: List<com.lbo.quran.ui.theme.FontOption>,
    selectedKey: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.key == selectedKey }?.label ?: options.first().label

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text("فونت") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, fontFamily = option.family) },
                    onClick = {
                        onSelect(option.key)
                        expanded = false
                    }
                )
            }
        }
    }
}

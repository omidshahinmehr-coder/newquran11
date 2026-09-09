package com.lbo.quran.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lbo.quran.audio.AudioProgress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioSettingsScreen(
    viewModel: QuranViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val downloadState by viewModel.audioDownload.collectAsState()
    val availableCount by viewModel.availableAudioCount.collectAsState()
    var urlInput by remember(settings.audioZipUrl) { mutableStateOf(settings.audioZipUrl) }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshAudioCount() }

    val pickZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importAudioFromLocalZip(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("صدای تلاوت") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(
                "فایل‌های صوتی $availableCount از ۶۲۳۶ آیه در حال حاضر روی دستگاه موجود است.",
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(20.dp))
            Text("دانلود از سایت", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("آدرس فایل zip صداها") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.updateSettings(settings.copy(audioZipUrl = urlInput))
                    viewModel.downloadAudio(urlInput)
                },
                enabled = urlInput.isNotBlank() && downloadState !is AudioProgress.Downloading && downloadState !is AudioProgress.Extracting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("دانلود و نصب صداها")
            }

            Spacer(Modifier.height(20.dp))
            Text("یا انتخاب فایل zip از حافظه‌ی گوشی", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { pickZipLauncher.launch(arrayOf("application/zip", "application/x-zip-compressed")) },
                enabled = downloadState !is AudioProgress.Downloading && downloadState !is AudioProgress.Extracting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("انتخاب فایل zip")
            }

            when (val ds = downloadState) {
                is AudioProgress.Downloading -> {
                    Spacer(Modifier.height(20.dp))
                    Text("در حال دانلود… ${(ds.fraction * 100).toInt()}٪")
                    LinearProgressIndicator(progress = { ds.fraction }, modifier = Modifier.fillMaxWidth())
                }
                is AudioProgress.Extracting -> {
                    Spacer(Modifier.height(20.dp))
                    Text("در حال استخراج فایل‌ها… ${(ds.fraction * 100).toInt()}٪")
                    LinearProgressIndicator(progress = { ds.fraction }, modifier = Modifier.fillMaxWidth())
                }
                is AudioProgress.Done -> {
                    Spacer(Modifier.height(20.dp))
                    Text(
                        "${ds.fileCount} فایل صوتی با موفقیت نصب شد.",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                is AudioProgress.Error -> {
                    Spacer(Modifier.height(20.dp))
                    Text("خطا: ${ds.message}", color = MaterialTheme.colorScheme.error)
                }
                null -> {}
            }

            if (availableCount > 0) {
                Spacer(Modifier.height(28.dp))
                OutlinedButton(
                    onClick = { showClearConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("حذف همه‌ی فایل‌های صوتی")
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("حذف فایل‌های صوتی") },
            text = { Text("همه‌ی فایل‌های صوتی نصب‌شده روی دستگاه حذف شوند؟") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearAllAudio()
                    viewModel.clearAudioDownloadStatus()
                    showClearConfirm = false
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("انصراف") }
            }
        )
    }
}

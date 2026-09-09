package com.lbo.quran

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lbo.quran.audio.AudioPlaybackController
import com.lbo.quran.audio.AudioRepository
import com.lbo.quran.data.QuranRepository
import com.lbo.quran.data.ReadingProgressRepository
import com.lbo.quran.data.SettingsRepository
import com.lbo.quran.ui.AboutScreen
import com.lbo.quran.ui.AudioSettingsScreen
import com.lbo.quran.ui.BookmarksScreen
import com.lbo.quran.ui.HizbPickerScreen
import com.lbo.quran.ui.HomeScreen
import com.lbo.quran.ui.JuzPickerScreen
import com.lbo.quran.ui.QuranViewModel
import com.lbo.quran.ui.SearchScreen
import com.lbo.quran.ui.SettingsScreen
import com.lbo.quran.ui.SplashScreen
import com.lbo.quran.ui.SurahPickerScreen
import com.lbo.quran.ui.TafsirBrowseScreen
import com.lbo.quran.ui.TafsirScreen
import com.lbo.quran.ui.theme.AppTypography
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(typography = AppTypography) {
                Surface(modifier = Modifier) {
                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                        QuranApp()
                    }
                }
            }
        }
    }
}

@Composable
fun QuranApp() {
    val context = LocalContext.current
    val repo = remember { QuranRepository(context.applicationContext) }
    val settingsRepo = remember { SettingsRepository(context.applicationContext) }
    val progressRepo = remember { ReadingProgressRepository(context.applicationContext) }
    val audioRepo = remember { AudioRepository(context.applicationContext) }
    val audioController = remember { AudioPlaybackController(context.applicationContext, audioRepo) }
    val viewModel: QuranViewModel = viewModel(
        factory = viewModelFactory(repo, settingsRepo, progressRepo, audioRepo, audioController)
    )
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onFinished = {
                    navController.navigate("home") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                viewModel = viewModel,
                onOpenSurahPicker = { navController.navigate("surahPicker") },
                onOpenJuzPicker = { navController.navigate("juzPicker") },
                onOpenHizbPicker = { navController.navigate("hizbPicker") },
                onOpenSearch = { navController.navigate("search") },
                onOpenTafsirBrowse = { navController.navigate("tafsirBrowse") },
                onOpenSettings = { navController.navigate("settings") },
                onOpenAudioSettings = { navController.navigate("audioSettings") },
                onOpenAbout = { navController.navigate("about") },
                onOpenBookmarks = { navController.navigate("bookmarks") },
                onOpenTafsir = { aId, surahName, ayahNumber ->
                    val encodedName = URLEncoder.encode(surahName, "UTF-8")
                    navController.navigate("tafsir/$aId/$encodedName/$ayahNumber")
                }
            )
        }

        composable("bookmarks") {
            BookmarksScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenAyah = { navController.popBackStack() },
                onOpenTafsirEntry = { surahNumber, language, tafsirId ->
                    viewModel.openTafsirEntry(surahNumber, language, tafsirId)
                    navController.navigate("tafsirBrowse") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable("surahPicker") {
            SurahPickerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSurahSelected = { surahNumber ->
                    viewModel.requestScrollToSurah(surahNumber)
                    navController.popBackStack()
                }
            )
        }

        composable("juzPicker") {
            JuzPickerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onJuzSelected = { juzNumber ->
                    viewModel.requestScrollToJuz(juzNumber)
                    navController.popBackStack()
                }
            )
        }

        composable("hizbPicker") {
            HizbPickerScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onHizbSelected = { hizbNumber ->
                    viewModel.requestScrollToHizb(hizbNumber)
                    navController.popBackStack()
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("audioSettings") {
            AudioSettingsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }

        composable("about") {
            AboutScreen(onBack = { navController.popBackStack() })
        }

        composable("tafsirBrowse") {
            TafsirBrowseScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenSearch = { navController.navigate("search") }
            )
        }

        composable("search") {
            SearchScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onOpenAyah = { aId ->
                    viewModel.requestScrollToAyah(aId)
                    navController.popBackStack("home", inclusive = false)
                },
                onOpenTafsirEntry = { surahNumber, language, tafsirId ->
                    viewModel.openTafsirEntry(surahNumber, language, tafsirId)
                    navController.navigate("tafsirBrowse") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            )
        }

        composable(
            "tafsir/{aId}/{surahName}/{ayahNumber}",
            arguments = listOf(
                navArgument("aId") { type = NavType.StringType },
                navArgument("surahName") { type = NavType.StringType },
                navArgument("ayahNumber") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val aId = backStackEntry.arguments?.getString("aId") ?: ""
            val surahName = URLDecoder.decode(backStackEntry.arguments?.getString("surahName") ?: "", "UTF-8")
            val ayahNumber = backStackEntry.arguments?.getInt("ayahNumber") ?: 0
            TafsirScreen(
                viewModel = viewModel,
                aId = aId,
                surahName = surahName,
                ayahNumber = ayahNumber,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

private fun viewModelFactory(
    repo: QuranRepository,
    settingsRepo: SettingsRepository,
    progressRepo: ReadingProgressRepository,
    audioRepo: AudioRepository,
    audioController: AudioPlaybackController
) =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return QuranViewModel(repo, settingsRepo, progressRepo, audioRepo, audioController) as T
        }
    }

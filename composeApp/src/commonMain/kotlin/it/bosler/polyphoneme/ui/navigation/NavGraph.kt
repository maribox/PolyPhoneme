package it.bosler.polyphoneme.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import it.bosler.polyphoneme.ui.about.AboutScreen
import it.bosler.polyphoneme.ui.about.BuildInfo
import it.bosler.polyphoneme.ui.library.LibraryScreen
import it.bosler.polyphoneme.ui.reader.ReaderScreen
import it.bosler.polyphoneme.ui.settings.SettingsScreen
import kotlinx.coroutines.flow.MutableStateFlow

@Composable
fun PolyPhonemeNavHost(
    filePicker: @Composable ((String) -> Unit) -> (() -> Unit) = { { } },
    pendingEpubUri: MutableStateFlow<String?>? = null,
    buildInfo: BuildInfo = BuildInfo(),
    navController: NavHostController = rememberNavController(),
) {
    NavHost(navController = navController, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(
                onBookClick = { bookId, chapterIndex ->
                    navController.navigate(ReaderRoute(bookId, chapterIndex))
                },
                onSettingsClick = {
                    navController.navigate(SettingsRoute)
                },
                filePicker = filePicker,
                pendingUri = pendingEpubUri,
            )
        }
        composable<ReaderRoute> { backStackEntry ->
            val route = backStackEntry.toRoute<ReaderRoute>()
            ReaderScreen(
                bookId = route.bookId,
                initialChapterIndex = route.chapterIndex,
                onBack = { navController.popBackStack() },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAbout = { navController.navigate(AboutRoute) },
            )
        }
        composable<AboutRoute> {
            AboutScreen(
                buildInfo = buildInfo,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

package com.sobik.app.ui

import androidx.compose.runtime.Composable
import com.sobik.app.AppContainer
import com.sobik.app.ui.algorithms.AlgorithmDetailScreen
import com.sobik.app.ui.algorithms.AlgorithmLibraryScreen
import com.sobik.app.ui.guide.GuideListScreen
import com.sobik.app.ui.guide.LessonScreen
import com.sobik.app.ui.home.HomeScreen
import com.sobik.app.ui.othercubes.BigCubeGuideScreen
import com.sobik.app.ui.othercubes.OtherCubesScreen
import com.sobik.app.ui.puzzles.PuzzleCatalogScreen
import com.sobik.app.ui.puzzles.PuzzleDetailScreen
import com.sobik.app.ui.review.ManualInputScreen
import com.sobik.app.ui.review.ReviewScreen
import com.sobik.app.ui.scan.ScanScreen
import com.sobik.app.ui.scan.ScanSetupScreen
import com.sobik.app.ui.solve.SolveScreen
import com.sobik.app.ui.theme.SobikTheme

/** Root composable: maps the current [Screen] to its content. Platform code handles system back. */
@Composable
fun SobikApp(container: AppContainer, nav: Navigator) {
    SobikTheme {
        when (val screen = nav.current) {
            Screen.Home -> HomeScreen(container, nav)
            Screen.ScanSetup -> ScanSetupScreen(nav)
            is Screen.Scan -> ScanScreen(container, nav, screen)
            Screen.Review -> ReviewScreen(container, nav)
            Screen.ManualInput -> ManualInputScreen(container, nav)
            Screen.Solve -> SolveScreen(container, nav)
            Screen.Guide -> GuideListScreen(container, nav)
            is Screen.Lesson -> LessonScreen(container, nav, screen.id)
            Screen.Algorithms -> AlgorithmLibraryScreen(container, nav)
            is Screen.AlgorithmDetail -> AlgorithmDetailScreen(container, nav, screen.id)
            Screen.OtherCubes -> OtherCubesScreen(container, nav)
            is Screen.BigCubeGuide -> BigCubeGuideScreen(container, nav, screen.size)
            Screen.Puzzles -> PuzzleCatalogScreen(container, nav)
            is Screen.PuzzleDetail -> PuzzleDetailScreen(container, nav, screen.id)
        }
    }
}

package com.sobik.app.ui

import androidx.compose.runtime.mutableStateListOf
import com.sobik.model.CubeType
import com.sobik.model.Face
import com.sobik.scanner.ScanMode

/** App destinations. Plain classes keep navigation type-safe without a navigation library. */
sealed interface Screen {
    data object Home : Screen
    data object ScanSetup : Screen
    data class Scan(val sessionId: Long, val type: CubeType, val mode: ScanMode, val intervalMs: Long, val retakeFace: Face? = null) : Screen
    data object Review : Screen
    data object Solve : Screen
    data object Guide : Screen
    data class Lesson(val id: String) : Screen
    data object Algorithms : Screen
    data class AlgorithmDetail(val id: String) : Screen
    data object OtherCubes : Screen
    data class BigCubeGuide(val size: Int) : Screen
    data object ManualInput : Screen
}

class Navigator {
    private val stack = mutableStateListOf<Screen>(Screen.Home)
    val current: Screen get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun push(screen: Screen) { stack.add(screen) }

    fun pop(): Boolean {
        if (stack.size <= 1) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    /** Replace the current screen (e.g. scan -> review, so back doesn't return to the camera). */
    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun popToHomeAnd(screen: Screen? = null) {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
        if (screen != null) stack.add(screen)
    }
}

package com.sobik.app.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sobik.app.AppContainer
import com.sobik.engine.CubeValidator
import com.sobik.model.CubeState
import com.sobik.model.FixSuggestion
import com.sobik.model.ScanResult
import com.sobik.model.StickerColor
import com.sobik.model.StickerRef
import com.sobik.model.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReviewUiState(
    val state: CubeState,
    /** Stickers whose recognition was uncertain and that the user has not confirmed/edited yet. */
    val unconfirmed: Set<StickerRef>,
    val validation: ValidationResult? = null,
    val validating: Boolean = true,
    val selected: StickerRef? = null,
    val edits: Int = 0,
) {
    val canSolve: Boolean get() = validation?.isValid == true && !validating
}

/**
 * Correction screen logic: every edit re-runs validation (off the main thread). Solving is only
 * possible once the state is valid; low-confidence stickers are surfaced first.
 */
class ReviewViewModel(private val container: AppContainer, scan: ScanResult) : ViewModel() {
    private val confidence: FloatArray = scan.confidenceArray()
    private val _ui = MutableStateFlow(ReviewUiState(scan.state, scan.lowConfidence().toSet()))
    val ui: StateFlow<ReviewUiState> = _ui.asStateFlow()
    private var validationJob: Job? = null

    init { revalidate() }

    fun select(ref: StickerRef) = _ui.update { it.copy(selected = if (it.selected == ref) null else ref) }

    fun setColor(color: StickerColor) {
        val ref = _ui.value.selected ?: return
        val n = _ui.value.state.size
        confidence[ref.globalIndex(n)] = 1f
        _ui.update { it.copy(state = it.state.with(ref, color), unconfirmed = it.unconfirmed - ref, edits = it.edits + 1) }
        revalidate()
    }

    fun confirmSelected() {
        val ref = _ui.value.selected ?: return
        confidence[ref.globalIndex(_ui.value.state.size)] = 1f
        _ui.update { it.copy(unconfirmed = it.unconfirmed - ref, selected = null) }
    }

    fun confirmAll() = _ui.update { it.copy(unconfirmed = emptySet()) }

    fun apply(suggestion: FixSuggestion) {
        var s = _ui.value.state
        for (f in suggestion.fixes) {
            s = s.with(f.sticker, f.to)
            confidence[f.sticker.globalIndex(s.size)] = 1f
        }
        val touched = suggestion.fixes.map { it.sticker }.toSet()
        _ui.update { it.copy(state = s, unconfirmed = it.unconfirmed - touched, selected = null, edits = it.edits + 1) }
        revalidate()
    }

    private fun revalidate() {
        validationJob?.cancel()
        val state = _ui.value.state
        val conf = confidence.copyOf()
        _ui.update { it.copy(validating = true) }
        validationJob = viewModelScope.launch {
            val result = withContext(Dispatchers.Default) { CubeValidator.validate(state, conf) }
            _ui.update { if (it.state == state) it.copy(validation = result, validating = false) else it }
        }
    }

    /** Persists the validated state; returns false if it is not valid. */
    fun accept(): Boolean {
        val ui = _ui.value
        if (!ui.canSolve) return false
        viewModelScope.launch(Dispatchers.IO) { container.cubeSession.confirm(ui.state) }
        container.cubeSession.select(ui.state)
        return true
    }
}

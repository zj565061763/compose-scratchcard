package com.sd.lib.compose.scratchcard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.util.fastCoerceAtMost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch

@Composable
fun rememberScratchcardState(
  /** The threshold for clearing the overlay. */
  clearThreshold: Float = 0.5f,
  initialCleared: Boolean = false,
  onScratchStart: () -> Boolean = { true },
): ScratchcardState {
  require(clearThreshold > 0f)
  return rememberSaveable(saver = ScratchcardState.Saver) {
    ScratchcardState(initialCleared)
  }.apply {
    this.coroutineScope = rememberCoroutineScope()
    this.onScratchStart = onScratchStart
    this.clearThreshold = clearThreshold.fastCoerceAtMost(1f)
  }
}

class ScratchcardState internal constructor(
  initialCleared: Boolean,
) {
  /** Whether the overlay has been cleared. */
  var cleared by mutableStateOf(initialCleared)
    private set

  internal val path = Path()

  private val _offsetFlow = MutableStateFlow<Offset?>(null)
  internal var offset by mutableStateOf<Offset?>(null)
    private set

  private val _forceRecomposeFlow = MutableStateFlow(false)
  internal var forceRecompose by mutableStateOf(false)
    private set

  internal lateinit var coroutineScope: CoroutineScope
  internal lateinit var onScratchStart: () -> Boolean
  internal var clearThreshold: Float = 0f
  internal lateinit var graphicsLayer: GraphicsLayer

  private var _canDrag = false
  private var _calculateJob: Job? = null

  /** Clear the overlay */
  fun clear() {
    cleared = true
    cancelCalculate()
  }

  /** Reset and display the overlay */
  fun reset() {
    cleared = false
    path.reset()
    offset = null
    cancelCalculate()
  }

  internal fun onDragStart(value: Offset) {
    if (cleared) return
    _canDrag = onScratchStart()
    if (_canDrag) {
      path.moveTo(value.x, value.y)
      updateOffset(value)
      startCalculate()
    }
  }

  internal fun onDragAmount(value: Offset) {
    if (cleared) return
    if (_canDrag) {
      offset?.also { current ->
        path.relativeLineTo(value.x, value.y)
        updateOffset(current + value)
      }
    }
  }

  internal fun reportDraw() {
    if (cleared) return
    _forceRecomposeFlow.value = false
  }

  private fun updateOffset(newOffset: Offset) {
    if (offset != newOffset) {
      _forceRecomposeFlow.value = true
      offset = newOffset
      _offsetFlow.value = newOffset
    }
  }

  @OptIn(FlowPreview::class)
  private fun startCalculate() {
    if (_calculateJob?.isActive == true) return
    _calculateJob = coroutineScope.launch(Dispatchers.Default) {
      coroutineScope {
        launch {
          _offsetFlow
            .sample(500)
            .collect { calculateProgress() }
        }
        launch {
          _forceRecomposeFlow
            .debounce(48)
            .collect {
              if (it) {
                forceRecompose = true
              }
            }
        }
      }
    }
  }

  private suspend fun calculateProgress() {
    val bitmap = graphicsLayer.toImageBitmap()

    val totalCount = bitmap.width * bitmap.height
    val progress = if (totalCount > 0) {
      IntArray(totalCount)
        .also { bitmap.readPixels(it) }
        .fold(0) { acc, pixel -> if (pixel == 0) acc + 1 else acc }
        .let { it.toFloat() / totalCount }
    } else {
      0f
    }

    if (progress >= clearThreshold) {
      clear()
    }
  }

  private fun cancelCalculate() {
    _calculateJob?.cancel()
    _calculateJob = null
    _offsetFlow.value = null
    _forceRecomposeFlow.value = false
    forceRecompose = false
  }

  companion object {
    internal val Saver = listSaver(
      save = { listOf(it.cleared) },
      restore = { ScratchcardState(initialCleared = it[0]) },
    )
  }
}
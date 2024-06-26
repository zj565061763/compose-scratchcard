package com.sd.lib.compose.scratchcard

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@SuppressLint("ComposableNaming")
@Composable
fun FScratchcardState.touchSpan(
    xSpanCount: Int,
    ySpanCount: Int,
    onTouchSpan: (row: Int, column: Int, touchCount: Int) -> Unit,
) {
    val state = this@touchSpan

    val onTouchSpanUpdated by rememberUpdatedState(onTouchSpan)
    val coroutineScope = rememberCoroutineScope()

    val touchHelper = remember(state, xSpanCount, ySpanCount, coroutineScope) {
        state.reset()
        object : TouchHelper(
            xSpanCount = xSpanCount,
            ySpanCount = ySpanCount,
            coroutineScope = coroutineScope,
        ) {
            override fun onTouchSpan(row: Int, column: Int, touchCount: Int) {
                if (!state.cleared) {
                    onTouchSpanUpdated(row, column, touchCount)
                }
            }
        }
    }

    touchHelper.setData(
        boxSize = state.boxSize,
        offset = state.offset,
    )
}

private abstract class TouchHelper(
    private val xSpanCount: Int,
    private val ySpanCount: Int,
    coroutineScope: CoroutineScope,
) {
    private val _totalSpanCount = xSpanCount * ySpanCount
    private val _dataFlow = MutableStateFlow(TouchData())

    private var _init = false
    private val _spans = mutableListOf<Pair<Int, Int>>()

    init {
        require(xSpanCount > 0)
        require(ySpanCount > 0)
    }

    private fun init() {
        if (!_init) {
            _init = true
            for (row in 0..<ySpanCount) {
                for (column in 0..<xSpanCount) {
                    _spans.add(row to column)
                }
            }
        }
    }

    private fun reset() {
        if (_init) {
            _init = false
            _spans.clear()
        }
    }

    fun setData(
        boxSize: Size?,
        offset: Offset?,
    ) {
        if (offset == null) {
            reset()
        }

        _dataFlow.value = TouchData(
            boxSize = boxSize,
            offset = offset,
        )
    }

    private fun calculate(data: TouchData) {
        val boxSize = data.boxSize ?: return
        val width = boxSize.width.takeIf { it > 0 } ?: return
        val height = boxSize.height.takeIf { it > 0 } ?: return

        val offset = data.offset ?: return

        init()
        if (_spans.isEmpty()) return

        val spanWidth = (width / xSpanCount).coerceAtLeast(1f)
        val spanHeight = (height / ySpanCount).coerceAtLeast(1f)

        with(_spans.iterator()) {
            while (hasNext()) {
                val (row, column) = next()

                val topLeft = Offset(column * spanWidth, row * spanHeight)
                val bottomRight = topLeft + Offset(spanWidth, spanHeight)

                val spanRect = Rect(topLeft = topLeft, bottomRight = bottomRight)
                if (spanRect.contains(offset)) {
                    remove()
                    onTouchSpan(
                        row = row,
                        column = column,
                        touchCount = _totalSpanCount - _spans.size
                    )
                    break
                }
            }
        }
    }

    init {
        coroutineScope.launch {
            _dataFlow.collect {
                calculate(it)
            }
        }
    }

    protected abstract fun onTouchSpan(
        row: Int,
        column: Int,
        touchCount: Int,
    )

    private data class TouchData(
        val boxSize: Size? = null,
        val offset: Offset? = null,
    )
}
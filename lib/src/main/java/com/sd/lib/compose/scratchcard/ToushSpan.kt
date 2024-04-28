package com.sd.lib.compose.scratchcard

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("ComposableNaming")
@Composable
fun FScratchcardState.touchSpan(
    xSpanCount: Int,
    ySpanCount: Int,
    onTouchSpan: (row: Int, column: Int, touchCount: Int) -> Unit,
) {
    val state = this@touchSpan
    val onTouchSpanUpdated by rememberUpdatedState(onTouchSpan)

    val touchHelper = remember(xSpanCount, ySpanCount) {
        state.reset()
        object : TouchHelper(xSpanCount = xSpanCount, ySpanCount = ySpanCount) {
            override fun onTouchSpan(row: Int, column: Int, touchCount: Int) {
                onTouchSpanUpdated(row, column, touchCount)
            }
        }
    }

    val coroutineScope = rememberCoroutineScope(getContext = { Dispatchers.IO })

    val offset = state.offset
    LaunchedEffect(touchHelper, offset) {
        coroutineScope.launch {
            touchHelper.setOffset(offset)
        }
    }

    val thickness = state.thickness
    LaunchedEffect(touchHelper, thickness) {
        coroutineScope.launch {
            touchHelper.setThickness(thickness)
        }
    }

    state.boxSize?.let { boxSize ->
        LaunchedEffect(touchHelper, boxSize) {
            coroutineScope.launch {
                touchHelper.setSize(boxSize.width, boxSize.height)
            }
        }
    }
}

private abstract class TouchHelper(
    private val xSpanCount: Int,
    private val ySpanCount: Int,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    private val _dispatcher = Dispatchers.Default.limitedParallelism(1)
    private val _totalSpanCount = xSpanCount * ySpanCount

    private var _init = false
    private val _spans = mutableListOf<Pair<Int, Int>>()

    private var _width = 0f
    private var _height = 0f

    private var _offset: Offset? = null
    private var _thickness: Float? = null

    init {
        require(xSpanCount > 0)
        require(ySpanCount > 0)
    }

    private fun init() {
        if (!_init) {
            _init = true
            for (row in 0..<xSpanCount) {
                for (column in 0..<ySpanCount) {
                    _spans.add(row to column)
                }
            }
        }
    }

    private fun reset() {
        _spans.clear()
        _init = false
    }

    suspend fun setSize(width: Float, height: Float) {
        withContext(_dispatcher) {
            _width = width
            _height = height
            calculate()
        }
    }

    suspend fun setOffset(offset: Offset?) {
        withContext(_dispatcher) {
            if (_offset != null && offset == null) {
                reset()
            }
            _offset = offset
            calculate()
        }
    }

    suspend fun setThickness(thickness: Float?) {
        withContext(_dispatcher) {
            _thickness = thickness
            calculate()
        }
    }

    private suspend fun calculate() {
        val width = _width.takeIf { it > 0 } ?: return
        val height = _height.takeIf { it > 0 } ?: return

        val offset = _offset ?: return
        val thickness = _thickness ?: return

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
                val touchRect = Rect(center = offset, radius = thickness / 2)

                if (spanRect.overlaps(touchRect)) {
                    remove()
                    withContext(Dispatchers.Main) {
                        onTouchSpan(
                            row = row,
                            column = column,
                            touchCount = _totalSpanCount - _spans.size
                        )
                    }
                }
            }
        }
    }

    protected abstract fun onTouchSpan(
        row: Int,
        column: Int,
        touchCount: Int,
    )
}
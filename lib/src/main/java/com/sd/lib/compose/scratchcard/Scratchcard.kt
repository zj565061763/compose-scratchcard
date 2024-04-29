package com.sd.lib.compose.scratchcard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize

@Composable
fun FScratchcard(
    state: FScratchcardState,
    modifier: Modifier = Modifier,
    thickness: Dp = 36.dp,
    /** 覆盖层 */
    overlay: @Composable BoxScope.() -> Unit,
    /** 内容层 */
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        content()
        AnimatedVisibility(
            visible = !state.cleared,
            modifier = Modifier.matchParentSize(),
        ) {
            ScratchcardBox(
                modifier = Modifier.matchParentSize(),
                state = state,
                thickness = thickness,
                content = overlay,
            )
        }
    }
}

@Composable
private fun ScratchcardBox(
    modifier: Modifier = Modifier,
    state: FScratchcardState,
    thickness: Dp,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    val thicknessPx by remember(density, thickness) {
        mutableFloatStateOf(with(density) { thickness.toPx() })
    }

    var boxSize by remember { mutableStateOf<Size?>(null) }
    state.boxSize = boxSize

    Box(
        modifier = modifier
            .onSizeChanged {
                boxSize = it.toSize()
            }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .pointerInput(state) {
                detectDragGestures(
                    onDragStart = { offset ->
                        state.onDragStart(offset)
                    },
                    onDrag = { _, dragAmount ->
                        state.onDrag(dragAmount)
                    },
                )
            }
            .drawWithCache {
                state.offset?.let { offset ->
                    state.path.lineTo(offset.x, offset.y)
                }
                onDrawWithContent {
                    drawContent()
                    drawPath(
                        path = state.path,
                        color = Color.Black,
                        style = Stroke(
                            width = thicknessPx,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                        blendMode = BlendMode.Clear,
                    )
                }
            }
    ) {
        content()
    }
}

@Composable
fun rememberFScratchcardState(
    onScratchStart: () -> Boolean = { true },
): FScratchcardState {
    return remember {
        FScratchcardState()
    }.also {
        it.onScratchStart = onScratchStart
    }
}

class FScratchcardState internal constructor() {
    internal var path by mutableStateOf(Path())
        private set

    /** 是否已经清空图片 */
    var cleared by mutableStateOf(false)
        private set

    /** 当前触摸的位置 */
    var offset by mutableStateOf<Offset?>(null)
        private set

    /** 容器大小 */
    var boxSize by mutableStateOf<Size?>(null)
        internal set

    lateinit var onScratchStart: () -> Boolean
    private var _canDrag = false

    internal fun onDragStart(offset: Offset) {
        _canDrag = onScratchStart()
        if (_canDrag) {
            this.path.moveTo(offset.x, offset.y)
            this.offset = offset
        }
    }

    internal fun onDrag(dragAmount: Offset) {
        if (_canDrag) {
            this.offset?.let { offset ->
                this.offset = offset + dragAmount
            }
        }
    }

    /**
     * 清空图片
     */
    fun clear() {
        this.cleared = true
    }

    /**
     * 重置图片
     */
    fun reset() {
        this.path.reset()
        this.offset = null
        this.cleared = false
    }
}
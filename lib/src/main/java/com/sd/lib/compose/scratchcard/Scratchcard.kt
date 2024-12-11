package com.sd.lib.compose.scratchcard

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScratchcardBox(
  modifier: Modifier = Modifier,
  state: ScratchcardState = rememberScratchcardState(),
  thickness: Dp = 36.dp,
  overlay: @Composable BoxScope.() -> Unit,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier,
    propagateMinConstraints = true,
  ) {
    content()
    ScratchcardOverlay(
      modifier = Modifier.matchParentSize(),
      state = state,
      thickness = thickness,
      overlay = overlay,
    )
  }
}

@Composable
fun ScratchcardOverlay(
  modifier: Modifier = Modifier,
  state: ScratchcardState = rememberScratchcardState(),
  thickness: Dp = 36.dp,
  overlay: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier.scratchcard(state, thickness),
    propagateMinConstraints = true,
  ) {
    if (state.cleared) {
      // Show nothing
    } else {
      overlay()
    }
  }
}

private fun Modifier.scratchcard(
  state: ScratchcardState,
  thickness: Dp = 36.dp,
): Modifier {
  if (state.cleared) return this
  return composed {
    val density = LocalDensity.current
    val thicknessPx = remember(density, thickness) { with(density) { thickness.toPx() } }

    /**
     * On some phones, the offset change doesn't trigger a redraw,
     * so a forced recomposition is required.
     */
    if (state.forceRecomposition) {
      state.redraw
    }

    scratchcardGesture(state)
      .drawWithContent {
        val graphicsLayer = state.graphicsLayer
        graphicsLayer.record { this@drawWithContent.drawContent() }
        drawLayer(graphicsLayer)
      }
      .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
      .drawWithContent {
        state.redraw
        state.reportDraw()
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
}

private fun Modifier.scratchcardGesture(
  state: ScratchcardState,
): Modifier = pointerInput(state) {
  detectDragGestures(
    onDragStart = { offset ->
      state.onDragStart(offset)
    },
    onDrag = { _, dragAmount ->
      state.onDragAmount(dragAmount)
    },
  )
}
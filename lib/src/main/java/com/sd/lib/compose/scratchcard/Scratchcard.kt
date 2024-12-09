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
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ScratchcardBox(
  state: ScratchcardState,
  modifier: Modifier = Modifier,
  thickness: Dp = 36.dp,
  overlay: @Composable BoxScope.() -> Unit,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier = modifier,
    propagateMinConstraints = true,
  ) {
    content()
    Box(
      modifier = Modifier
        .matchParentSize()
        .scratchcard(state, thickness),
      propagateMinConstraints = true,
    ) {
      if (!state.cleared) {
        overlay()
      }
    }
  }
}

fun Modifier.scratchcard(
  state: ScratchcardState,
  thickness: Dp = 36.dp,
): Modifier {
  if (state.cleared) return this
  return composed {
    val density = LocalDensity.current
    val thicknessPx = remember(density, thickness) { with(density) { thickness.toPx() } }

    val graphicsLayer = rememberGraphicsLayer()
    state.graphicsLayer = graphicsLayer

    if (state.forceRecompose) {
      /**
       * On some phones, after a long press,
       * the offset change doesn't trigger a redraw, so a forced recomposition is required.
       */
      state.offset
    }

    scratchcardGesture(state)
      .drawWithContent {
        graphicsLayer.record { this@drawWithContent.drawContent() }
        drawLayer(graphicsLayer)
      }
      .graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
      }
      .drawWithContent {
        drawContent()
        state.reportDraw()
        state.offset?.also {
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
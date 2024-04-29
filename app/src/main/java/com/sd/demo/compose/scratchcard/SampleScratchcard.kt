package com.sd.demo.compose.scratchcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import com.sd.demo.compose.scratchcard.theme.AppTheme
import com.sd.lib.compose.scratchcard.FScratchcard
import com.sd.lib.compose.scratchcard.rememberFScratchcardState
import com.sd.lib.compose.scratchcard.touchSpan

class SampleScratchcard : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                ContentView()
            }
        }
    }
}

@Composable
private fun ContentView(
    modifier: Modifier = Modifier,
) {
    val state = rememberFScratchcardState()

    state.touchSpan(xSpanCount = 3, ySpanCount = 3) { row, column, touchCount ->
        logMsg { "($row,$column) $touchCount" }
        if (touchCount == 5) {
            state.clear()
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = { state.clear() }) {
            Text(text = "Clear")
        }
        Button(onClick = { state.reset() }) {
            Text(text = "Reset")
        }

        FScratchcard(
            image = ImageBitmap.imageResource(R.drawable.scratchcard_top_image),
            state = state,
        ) {
            Image(
                painter = painterResource(R.drawable.scratchcard_bottom_image),
                contentDescription = null,
            )
        }
    }
}
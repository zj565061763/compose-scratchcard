package com.sd.demo.compose.scratchcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sd.demo.compose.scratchcard.theme.AppTheme
import com.sd.lib.compose.scratchcard.ScratchcardBox
import com.sd.lib.compose.scratchcard.rememberScratchcardState

class Sample : ComponentActivity() {
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
  val state = rememberScratchcardState(
    onScratchStart = {
      logMsg { "onScratchStart" }
      true
    },
  )

  Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Button(onClick = { state.clear() }) {
      Text(text = "Clear")
    }
    Button(onClick = { state.reset() }) {
      Text(text = "Reset")
    }
    ScratchcardBox(
      state = state,
      overlay = {
        Box(modifier.background(Color.Gray))
      },
      content = {
        Image(
          painter = painterResource(R.drawable.scratchcard_content),
          contentDescription = null,
        )
      },
    )
  }
}
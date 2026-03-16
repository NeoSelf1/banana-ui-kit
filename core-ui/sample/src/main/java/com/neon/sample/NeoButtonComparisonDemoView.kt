package com.neon.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.neon.core.ui.component.button.NeoClickable
import com.neon.core.ui.theme.Gray50
import com.neon.core.ui.theme.Gray80
import com.neon.core.ui.theme.NeoFont
import com.neon.core.ui.theme.Primary10
import com.neon.core.ui.theme.Primary50
import com.neon.sample.component.NeoButtonLegacy

private const val BUTTON_COUNT = 60

private enum class Screen {
    Menu,
    NeoButton, NeoButtonLegacy,
    SingleNeoButton, SingleNeoButtonLegacy,
}

@Composable
fun NeoButtonComparisonDemoView() {
    var currentScreen by remember { mutableStateOf(Screen.Menu) }

    when (currentScreen) {
        Screen.Menu -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Button Performance Test",
                    style = NeoFont.headline3,
                )
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = { currentScreen = Screen.NeoButtonLegacy },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("NeoButtonLegacy")
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = { currentScreen = Screen.NeoButton },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("NeoButton")
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Single Button (LongClick Test)",
                    style = NeoFont.subhead3,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Button(
                        onClick = { currentScreen = Screen.SingleNeoButtonLegacy },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Single Legacy")
                    }
                    Button(
                        onClick = { currentScreen = Screen.SingleNeoButton },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Single Node")
                    }
                }
            }
        }

        Screen.NeoButton -> {
            BackHandler { currentScreen = Screen.Menu }
            NeoButtonScrollList()
        }

        Screen.NeoButtonLegacy -> {
            BackHandler { currentScreen = Screen.Menu }
            NeoButtonLegacyScrollList()
        }

        Screen.SingleNeoButton -> {
            BackHandler { currentScreen = Screen.Menu }
            SingleNeoButtonScreen()
        }

        Screen.SingleNeoButtonLegacy -> {
            BackHandler { currentScreen = Screen.Menu }
            SingleNeoButtonLegacyScreen()
        }
    }
}

@Composable
private fun NeoButtonScrollList() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(BUTTON_COUNT) { index ->
            NeoClickable(
                modifier = Modifier.fillMaxWidth(),
                action = {},
                transitionType = NeoClickable.TransitionType.ShrinkWithGrayBackground,
            ) {
                ButtonItemContent(index = index, transitionTypeName = "ShrinkWithGrayBackground")
            }
        }
    }
}

@Composable
private fun NeoButtonLegacyScrollList() {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(BUTTON_COUNT) { index ->
            NeoButtonLegacy(
                modifier = Modifier.fillMaxWidth(),
                action = {},
                transitionType = NeoButtonLegacy.TransitionType.ShrinkWithGrayBackground,
            ) {
                ButtonItemContent(index = index, transitionTypeName = "ShrinkWithGrayBackground")
            }
        }
    }
}

@Composable
private fun SingleNeoButtonScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        NeoClickable(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            action = {},
            transitionType = NeoClickable.TransitionType.ShrinkWithGrayBackground,
        ) {
            ButtonItemContent(index = 0, transitionTypeName = "ShrinkWithGrayBackground")
        }
    }
}

@Composable
private fun SingleNeoButtonLegacyScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        NeoButtonLegacy(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            action = {},
            transitionType = NeoButtonLegacy.TransitionType.ShrinkWithGrayBackground,
        ) {
            ButtonItemContent(index = 0, transitionTypeName = "ShrinkWithGrayBackground")
        }
    }
}

@Composable
private fun ButtonItemContent(index: Int, transitionTypeName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Primary10),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${index + 1}",
                style = NeoFont.subhead5,
                color = Primary50
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Button ${index + 1}",
                style = NeoFont.subhead5,
                color = Gray80
            )
            Text(
                text = transitionTypeName,
                style = NeoFont.body4,
                color = Gray50
            )
        }
    }
}

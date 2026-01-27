package com.humanics.exampleapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.humanics.exampleapplication.ui.theme.ExampleApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ExampleApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DemoTabScreen()
                }
            }
        }
    }
}

/**
 * 레거시 코드와 개선된 코드를 비교할 수 있는 탭 화면
 */
@Composable
fun DemoTabScreen() {
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    val tabs = listOf(
        "Legacy DnD" to "기존 구현",
        "HMDraggableList" to "개선된 구현"
    )

    Column(modifier = Modifier
        .fillMaxSize()
        .onGloballyPositioned { coordinate ->
            println("부모뷰: ${coordinate.size}")
        }) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            modifier = Modifier.padding(top = 48.dp)
        ) {
            tabs.forEachIndexed { index, (title, subtitle) ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Column {
                            Text(text = title)
                            Text(
                                text = subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                )
            }
        }

        when (selectedTabIndex) {
            0 -> DragAndDropDemoView()
            1 -> HMDraggableListDemoView()
        }
    }
}

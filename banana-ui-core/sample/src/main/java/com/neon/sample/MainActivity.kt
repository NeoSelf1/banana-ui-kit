package com.neon.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.neon.core.ui.theme.Gray10
import com.neon.core.ui.theme.BananaDesign
import com.neon.core.ui.theme.Primary50

enum class DemoRoute(val title: String) {
    DraggableList("NeoDraggableList"),
    DragAndDrop("DragAndDrop"),
    ButtonComparison("NeoButton Comparison"),
    Picker("NeoPicker"),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Gray10
            ) {
                DemoNavHost()
            }
        }
    }
}

@Composable
fun DemoNavHost() {
    var currentRoute by remember { mutableStateOf<DemoRoute?>(null) }

    if (currentRoute == null) {
        DemoList(onNavigate = { currentRoute = it })
    } else {
        BackHandler { currentRoute = null }
        when (currentRoute) {
            DemoRoute.DraggableList -> NeoDraggableListDemoView()
            DemoRoute.DragAndDrop -> DragAndDropDemoView()
            DemoRoute.ButtonComparison -> NeoButtonComparisonDemoView()
            DemoRoute.Picker -> NeoPickerDemoView()
            null -> {}
        }
    }
}

@Composable
private fun DemoList(onNavigate: (DemoRoute) -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = "Core UI Samples",
            style = BananaDesign.typography.headline2,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
        )
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            items(DemoRoute.entries) { route ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigate(route) }
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                        .testTag("nav_${route.name}"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = route.title,
                        style = BananaDesign.typography.subhead4
                    )
                    Text(
                        text = "›",
                        style = BananaDesign.typography.subhead4,
                        color = Primary50
                    )
                }
                HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

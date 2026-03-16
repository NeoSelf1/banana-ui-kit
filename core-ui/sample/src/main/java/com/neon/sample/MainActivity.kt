package com.neon.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                DemoTabScreen()
            }
        }
    }
}

/**
 * 레거시 코드와 개선된 코드를 비교할 수 있는 탭 화면
 */
@Composable
fun DemoTabScreen() {
    Column(Modifier.fillMaxSize().padding(vertical = 64.dp)) {
//        HMButtonComparisonDemoView()
//        DragAndDropDemoView()
        HMPickerDemoView()
//        DragAndDropKeyComparisonDemoView()
    }
}

package dev.ijlal.stacks

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.ijlal.stacks.ui.StacksApp
import dev.ijlal.stacks.ui.theme.StacksTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StacksTheme {
                StacksApp()
            }
        }
    }
}

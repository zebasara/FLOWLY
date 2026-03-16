package com.flowly.move

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.flowly.move.ui.navigation.FlowlyNavGraph
import com.flowly.move.ui.theme.FlowlyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Status bar transparente + barra de navegación del color de fondo de la app
        // SystemBarStyle.dark → iconos blancos, fondo oscuro (#0A120A = FlowlyBg)
        enableEdgeToEdge(
            statusBarStyle     = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.parseColor("#0A120A"))
        )

        setContent {
            FlowlyTheme {
                FlowlyNavGraph()
            }
        }
    }
}

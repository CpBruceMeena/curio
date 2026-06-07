package com.curio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.curio.app.ui.navigation.CurioNavGraph
import com.curio.app.ui.theme.CurioTheme
import com.curio.app.ui.theme.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            CurioTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Surface
                ) {
                    val navController = rememberNavController()
                    CurioNavGraph(navController = navController)
                }
            }
        }
    }
}

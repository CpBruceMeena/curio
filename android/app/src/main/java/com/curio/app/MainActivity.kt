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
import com.curio.app.ui.theme.LightSurface
import com.curio.app.ui.theme.Surface

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme = CurioApp.darkThemeEnabled

            CurioTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = if (isDarkTheme) Surface else LightSurface
                ) {
                    val navController = rememberNavController()
                    CurioNavGraph(navController = navController)
                }
            }
        }
    }
}

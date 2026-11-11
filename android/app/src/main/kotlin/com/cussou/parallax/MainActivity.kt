package com.cussou.parallax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.cussou.parallax.ui.quickcalc.QuickCalcRoute
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParallaxPlannerTheme {
                QuickCalcRoute()
            }
        }
    }
}

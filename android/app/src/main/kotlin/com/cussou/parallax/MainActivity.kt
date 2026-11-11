package com.cussou.parallax

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.cussou.parallax.ui.theme.ParallaxPlannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ParallaxPlannerTheme {
                PlaceholderScreen()
            }
        }
    }
}

/** Temporary screen: the quick calc replaces it once it exists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScreen() {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.quick_calc_empty), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Preview
@Composable
private fun PlaceholderScreenPreview() {
    ParallaxPlannerTheme { PlaceholderScreen() }
}

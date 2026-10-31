package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.JokeRepository
import com.example.data.local.AppDatabase
import com.example.data.remote.JokeRemoteDataSource
import com.example.ui.AnekdotSearchApp
import com.example.ui.JokeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        val repository = JokeRepository(
            remoteDataSource = JokeRemoteDataSource(),
            jokeDao = database.jokeDao()
        )

        setContent {
            val viewModel: JokeViewModel = viewModel(
                factory = remember { JokeViewModel.provideFactory(repository) }
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = uiState.darkThemeOverride ?: systemDark

            MyApplicationTheme(darkTheme = effectiveDark) {
                AnekdotSearchApp(
                    viewModel = viewModel,
                    isDarkTheme = effectiveDark
                )
            }
        }
    }
}

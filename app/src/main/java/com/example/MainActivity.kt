package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.DashboardScreen
import com.example.ui.PdfReaderScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.PdfViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val pdfViewModel: PdfViewModel = viewModel()

                // Register handler for incoming intents (e.g., opened via file browser or email attachment)
                LaunchedEffect(intent) {
                    handleIntent(intent, pdfViewModel) {
                        navController.navigate("reader") {
                            popUpTo("dashboard") { saveState = true }
                            launchSingleTop = true
                        }
                    }
                }

                val errorMessage by pdfViewModel.errorMessage.collectAsState()
                val snackbarHostState = remember { SnackbarHostState() }

                // Safely show error alerts via bottom Snackbars
                LaunchedEffect(errorMessage) {
                    errorMessage?.let { errorMsg ->
                        snackbarHostState.showSnackbar(
                            message = errorMsg,
                            duration = SnackbarDuration.Short
                        )
                        pdfViewModel.clearError()
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "dashboard",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = pdfViewModel,
                                onNavigateToReader = {
                                    navController.navigate("reader")
                                }
                            )
                        }
                        composable("reader") {
                            PdfReaderScreen(
                                viewModel = pdfViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleIntent(intent: Intent?, viewModel: PdfViewModel, onOpenSuccess: () -> Unit) {
        if (intent == null) return
        val action = intent.action
        val data = intent.data
        if (Intent.ACTION_VIEW == action && data != null) {
            viewModel.openPdf(data, this) {
                onOpenSuccess()
            }
        }
    }
}

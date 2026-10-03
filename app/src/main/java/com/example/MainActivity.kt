package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.CalculatorViewModel
import com.example.ui.components.AboutDialog
import com.example.ui.components.AmoaTopBar
import com.example.ui.components.AppTab
import com.example.ui.components.SplashScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.TimeScreen
import com.example.ui.screens.UnitsScreen
import com.example.ui.theme.LocalAmoaColors
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CalculatorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsState()
            val currentTab by viewModel.currentTab.collectAsState()
            val isFunctionsOpen by viewModel.isFunctionsOpen.collectAsState()
            val showAbout by viewModel.showAboutDialog.collectAsState()
            val historyList by viewModel.historyList.collectAsState()
            val loadedExpr by viewModel.loadedSciExpr.collectAsState()

            var isSplashFinished by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                val colors = LocalAmoaColors.current

                if (!isSplashFinished) {
                    // Ultra-fast sub-1s start screen showing AMOA Calculator ... Developed by Babar Abbasi
                    SplashScreen(
                        onTimeout = { isSplashFinished = true }
                    )
                } else {
                    // BackHandler: return to BASIC calculator if on secondary screens
                    BackHandler(enabled = currentTab != AppTab.BASIC || isFunctionsOpen) {
                        if (isFunctionsOpen) {
                            viewModel.toggleFunctions(false)
                            viewModel.selectTab(AppTab.BASIC)
                        } else if (currentTab != AppTab.BASIC) {
                            viewModel.selectTab(AppTab.BASIC)
                        }
                    }

                    Scaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.pageBg),
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                                .background(colors.pageBg)
                        ) {
                            // Unified SINGLE-LINE top bar with Modes and About button
                            AmoaTopBar(
                                selectedTab = currentTab,
                                isFunctionsOpen = isFunctionsOpen,
                                isDarkTheme = isDarkTheme,
                                onTabSelected = { tab -> viewModel.selectTab(tab) },
                                onToggleTheme = { viewModel.toggleTheme() },
                                onOpenAbout = { viewModel.setAboutDialogVisible(true) }
                            )

                            // Main Screen Area (50/50 split for Calculator)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                when (currentTab) {
                                    AppTab.BASIC, AppTab.FUNCTIONS -> {
                                        CalculatorScreen(
                                            initialExpr = loadedExpr,
                                            isFunctionsOpen = isFunctionsOpen,
                                            onToggleFunctions = { viewModel.toggleFunctions(it) },
                                            onCommitHistory = { expr, res ->
                                                viewModel.addHistory("Scientific", expr, res)
                                            }
                                        )
                                    }
                                    AppTab.UNITS -> {
                                        UnitsScreen(
                                            onAddHistory = { mode, expr, res ->
                                                viewModel.addHistory(mode, expr, res)
                                            }
                                        )
                                    }
                                    AppTab.TIME -> {
                                        TimeScreen(
                                            onAddHistory = { mode, expr, res ->
                                                viewModel.addHistory(mode, expr, res)
                                            }
                                        )
                                    }
                                    AppTab.HISTORY -> {
                                        HistoryScreen(
                                            entries = historyList,
                                            onSelectHistoryExpr = { expr ->
                                                viewModel.loadExpressionForCalc(expr)
                                            },
                                            onDeleteEntry = { id -> viewModel.deleteEntry(id) },
                                            onUpdateNote = { id, note -> viewModel.updateNote(id, note) },
                                            onClearAll = { viewModel.clearAllHistory() },
                                            onImportEntries = { entries -> viewModel.importEntries(entries) }
                                        )
                                    }
                                }
                            }
                        }

                        // About dialog
                        if (showAbout) {
                            AboutDialog(
                                onDismissRequest = { viewModel.setAboutDialogVisible(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}

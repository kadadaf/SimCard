package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.receiver.SimKeepAliveReceiver
import com.example.ui.SimViewModel
import com.example.ui.SimViewModelFactory
import com.example.ui.screens.AddSimScreen
import com.example.ui.screens.BackupSettingsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DetailScreen
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale
import android.content.res.Configuration

sealed class Screen {
    object Dashboard : Screen()
    object AddSim : Screen()
    data class Detail(val simId: Int) : Screen()
}

class MainActivity : ComponentActivity() {

    private val viewModel: SimViewModel by viewModels {
        SimViewModelFactory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule periodic keep-alive check alarm triggers
        SimKeepAliveReceiver.schedulePeriodicCheck(this)

        setContent {
            val appLanguage by viewModel.appLanguage.collectAsState()
            val context = LocalContext.current
            val localizedContext = remember(appLanguage) {
                val locale = when (appLanguage) {
                    "en" -> Locale("en")
                    "zh" -> Locale("zh")
                    else -> Locale.getDefault()
                }
                Locale.setDefault(locale)
                val config = Configuration(context.resources.configuration)
                config.setLocale(locale)
                context.createConfigurationContext(config)
            }

            CompositionLocalProvider(LocalContext provides localizedContext) {
                MyApplicationTheme {
                    MainContainer(viewModel = viewModel, context = localizedContext)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainContainer(viewModel: SimViewModel, context: Context) {
    var currentTab by remember { mutableStateOf("Dashboard") }
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Dashboard) }

    val simCards by viewModel.simCards.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val history by viewModel.history.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    // Status notifications/toasts inside UI
    LaunchedEffect(statusMessage) {
        statusMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .testTag("bottom_nav_bar")
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = currentTab == "Dashboard",
                    onClick = {
                        currentTab = "Dashboard"
                        // Reset nested navigation to list when clicking home tab
                        currentScreen = Screen.Dashboard
                    },
                    icon = { Icon(Icons.Default.CreditCard, contentDescription = "Dashboard") },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.nav_dashboard)) },
                    modifier = Modifier.testTag("nav_dashboard")
                )

                NavigationBarItem(
                    selected = currentTab == "Settings",
                    onClick = { currentTab = "Settings" },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.nav_settings)) },
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()) // Respect navigation insets safely
        ) {
            AnimatedContent(
                targetState = Pair(currentTab, currentScreen),
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "MainCrossfade"
            ) { (tab, screen) ->
                if (tab == "Settings") {
                    BackupSettingsScreen(
                        viewModel = viewModel,
                        onTriggerCheck = {
                            // Fire exact broadcast to receivers instantly
                            val intent = Intent(context, SimKeepAliveReceiver::class.java)
                            context.sendBroadcast(intent)
                        },
                        onExportBackup = {
                            viewModel.exportBackup()
                        },
                        onImportBackup = { jsonStr ->
                            viewModel.importBackup(jsonStr)
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    when (screen) {
                        is Screen.Dashboard -> {
                            DashboardScreen(
                                simCards = simCards,
                                rules = rules,
                                onSelectSim = { sim ->
                                    currentScreen = Screen.Detail(sim.id)
                                },
                                onNavigateToAddSim = {
                                    currentScreen = Screen.AddSim
                                },
                                onNavigateToRules = {
                                    // Navigate internally
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is Screen.AddSim -> {
                            AddSimScreen(
                                onNavigateBack = {
                                    currentScreen = Screen.Dashboard
                                },
                                onSaveSim = { phoneNumber, operator, cardType, activationDate, plan, balance, currency, notes ->
                                    viewModel.addSimCard(
                                        phoneNumber, operator, cardType, activationDate, plan, balance, currency, notes
                                    )
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        is Screen.Detail -> {
                            val selectedSim = simCards.find { it.id == screen.simId }
                            if (selectedSim != null) {
                                val simRules = rules.filter { it.simId == selectedSim.id }
                                val simHistory = history.filter { it.simId == selectedSim.id }

                                DetailScreen(
                                    sim = selectedSim,
                                    rules = simRules,
                                    history = simHistory,
                                    onNavigateBack = {
                                        currentScreen = Screen.Dashboard
                                    },
                                    onDeleteSim = {
                                        viewModel.deleteSimCard(selectedSim)
                                        currentScreen = Screen.Dashboard
                                    },
                                    onRecordAction = { simId, ruleId, actionType, cost, notes ->
                                        viewModel.performKeepAliveAction(
                                            simId = simId,
                                            ruleId = ruleId,
                                            actionType = actionType,
                                            amountSpent = cost,
                                            description = "$actionType keeping-alive action registered. Details: $notes"
                                        )
                                    },
                                    onAddCustomRule = { newRule ->
                                        viewModel.addOrUpdateRule(newRule)
                                    },
                                    onDeleteCustomRule = { ruleToDelete ->
                                        viewModel.deleteRule(ruleToDelete)
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                // Fallback if SIM deleted in background
                                currentScreen = Screen.Dashboard
                            }
                        }
                    }
                }
            }
        }
    }
}

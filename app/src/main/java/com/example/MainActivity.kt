package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.ui.SurebetViewModel
import com.example.ui.screens.BlackjackScreen
import com.example.ui.screens.CalculatorScreen
import com.example.ui.screens.CasinoRolloverScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SurebetViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val state by viewModel.uiState.collectAsState()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar(modifier = Modifier.testTag("bottom_nav_bar")) {
                            NavigationBarItem(
                                selected = state.currentTab == 0,
                                onClick = { viewModel.selectTab(0) },
                                icon = { Icon(Icons.Default.TrendingUp, contentDescription = "Surebets") },
                                label = { Text("Surebets") },
                                modifier = Modifier.testTag("nav_tab_surebets")
                            )
                            NavigationBarItem(
                                selected = state.currentTab == 1,
                                onClick = { viewModel.selectTab(1) },
                                icon = { Icon(Icons.Default.Calculate, contentDescription = "Calculadora") },
                                label = { Text("Calculadora") },
                                modifier = Modifier.testTag("nav_tab_calculator")
                            )
                            NavigationBarItem(
                                selected = state.currentTab == 2,
                                onClick = { viewModel.selectTab(2) },
                                icon = { Icon(Icons.Default.Casino, contentDescription = "Casino EV") },
                                label = { Text("Casino EV") },
                                modifier = Modifier.testTag("nav_tab_casino")
                            )
                            NavigationBarItem(
                                selected = state.currentTab == 3,
                                onClick = { viewModel.selectTab(3) },
                                icon = { Icon(Icons.Default.Style, contentDescription = "Blackjack") },
                                label = { Text("Blackjack") },
                                modifier = Modifier.testTag("nav_tab_blackjack")
                            )
                        }
                    }
                ) { innerPadding ->
                    when (state.currentTab) {
                        0 -> DashboardScreen(
                            state = state,
                            onSearchChange = { viewModel.updateSearchQuery(it) },
                            onSportFilterChange = { viewModel.updateSportFilter(it) },
                            onMinProfitChange = { viewModel.updateMinProfitFilter(it) },
                            onBookmakerChange = { viewModel.updateBookmakerFilter(it) },
                            onRefresh = { viewModel.refreshScanner() },
                            onLoadIntoCalculator = { viewModel.loadIntoCalculator(it) },
                            modifier = Modifier.padding(innerPadding)
                        )
                        1 -> CalculatorScreen(
                            calcState = state.calculatorState,
                            onStakeChange = { viewModel.updateCalculatorStake(it) },
                            onThreeWayToggle = { viewModel.toggleThreeWay(it) },
                            onOddsFormatToggle = { viewModel.toggleOddsFormat(it) },
                            onRoundStakesToggle = { viewModel.toggleRoundStakes(it) },
                            onOutcome1Change = { n, b, o -> viewModel.updateCalcOutcome1(n, b, o) },
                            onOutcome2Change = { n, b, o -> viewModel.updateCalcOutcome2(n, b, o) },
                            onOutcome3Change = { n, b, o -> viewModel.updateCalcOutcome3(n, b, o) },
                            modifier = Modifier.padding(innerPadding)
                        )
                        2 -> CasinoRolloverScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                        3 -> BlackjackScreen(
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

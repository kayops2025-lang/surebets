package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MockOddsDataSource
import com.example.model.BetOutcome
import com.example.model.CalculationResult
import com.example.model.OddsFormat
import com.example.model.SportType
import com.example.model.SurebetOpportunity
import com.example.util.ArbitrageMath
import com.example.util.FreeBetResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

data class CalculatorUiState(
    val isThreeWay: Boolean = false,
    val oddsFormat: OddsFormat = OddsFormat.DECIMAL,
    val totalStake: Double = 100.0,
    val roundStakes: Boolean = true,
    val name1: String = "Equipo Local",
    val book1: String = "Pinnacle",
    val odds1: String = "2.10",
    val name2: String = "Equipo Visitante",
    val book2: String = "Bet365",
    val odds2: String = "2.05",
    val name3: String = "Empate (X)",
    val book3: String = "FanDuel",
    val odds3: String = "3.80",
    val result: CalculationResult? = null
)

data class PromoUiState(
    val promoAmount: Double = 50.0,
    val promoOdds: String = "2.50",
    val hedgeOdds: String = "1.77",
    val promoBook: String = "DraftKings",
    val hedgeBook: String = "FanDuel",
    val result: FreeBetResult? = null
)

data class SurebetUiState(
    val currentTab: Int = 0,
    val searchQuery: String = "",
    val selectedSport: SportType = SportType.ALL,
    val minProfit: Double = 0.0,
    val selectedBookmaker: String = "Todas",
    val isScanning: Boolean = false,
    val allOpportunities: List<SurebetOpportunity> = emptyList(),
    val filteredOpportunities: List<SurebetOpportunity> = emptyList(),
    val calculatorState: CalculatorUiState = CalculatorUiState(),
    val promoState: PromoUiState = PromoUiState(),
    val lastScanTimestamp: String = "Ahora mismo"
)

class SurebetViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SurebetUiState())
    val uiState: StateFlow<SurebetUiState> = _uiState.asStateFlow()

    init {
        val initialList = MockOddsDataSource.getInitialSurebets()
        _uiState.update {
            it.copy(
                allOpportunities = initialList,
                filteredOpportunities = initialList
            )
        }
        computeCalculator()
        computePromo()
    }

    fun selectTab(tab: Int) {
        _uiState.update { it.copy(currentTab = tab) }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters()
    }

    fun updateSportFilter(sport: SportType) {
        _uiState.update { it.copy(selectedSport = sport) }
        applyFilters()
    }

    fun updateMinProfitFilter(minProfit: Double) {
        _uiState.update { it.copy(minProfit = minProfit) }
        applyFilters()
    }

    fun updateBookmakerFilter(bookmaker: String) {
        _uiState.update { it.copy(selectedBookmaker = bookmaker) }
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        val filtered = state.allOpportunities.filter { arb ->
            val matchesSport = state.selectedSport == SportType.ALL || arb.sport == state.selectedSport
            val matchesProfit = arb.profitPercentage >= state.minProfit
            val matchesSearch = state.searchQuery.isBlank() ||
                    arb.eventName.contains(state.searchQuery, ignoreCase = true) ||
                    arb.outcomes.any { it.name.contains(state.searchQuery, ignoreCase = true) }
            val matchesBookmaker = state.selectedBookmaker == "Todas" ||
                    arb.outcomes.any { it.bookmaker.equals(state.selectedBookmaker, ignoreCase = true) }
            matchesSport && matchesProfit && matchesSearch && matchesBookmaker
        }
        _uiState.update { it.copy(filteredOpportunities = filtered) }
    }

    fun refreshScanner() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            delay(900) // Simulated network scan

            val current = _uiState.value.allOpportunities.toMutableList()
            // Randomly fluctuate or add slight odds variations
            val updated = current.map { arb ->
                val delta = (Random.nextDouble(-0.04, 0.05) * 100).toInt() / 100.0
                val newOutcomes = arb.outcomes.mapIndexed { idx, out ->
                    if (idx == 0) {
                        val newDec = maxOf(1.30, ((out.oddsDecimal + delta) * 100).toInt() / 100.0)
                        out.copy(oddsDecimal = newDec, oddsAmerican = ArbitrageMath.decimalToAmerican(newDec))
                    } else out
                }
                val invSum = newOutcomes.sumOf { 1.0 / it.oddsDecimal }
                val profit = if (invSum < 1.0) ((1.0 / invSum) - 1.0) * 100.0 else (1.0 - invSum) * 100.0
                arb.copy(
                    outcomes = newOutcomes,
                    profitPercentage = (profit * 100).toInt() / 100.0,
                    totalImpliedProbability = (invSum * 1000).toInt() / 10.0
                )
            }

            _uiState.update {
                it.copy(
                    isScanning = false,
                    allOpportunities = updated,
                    lastScanTimestamp = "Hace un momento"
                )
            }
            applyFilters()
        }
    }

    fun loadIntoCalculator(arb: SurebetOpportunity) {
        val o1 = arb.outcomes.getOrNull(0)
        val o2 = arb.outcomes.getOrNull(1)
        val o3 = arb.outcomes.getOrNull(2)

        _uiState.update { state ->
            state.copy(
                currentTab = 1, // switch to Calculator
                calculatorState = state.calculatorState.copy(
                    isThreeWay = arb.isThreeWay,
                    name1 = o1?.name ?: "Selección 1",
                    book1 = o1?.bookmaker ?: "Casa 1",
                    odds1 = o1?.oddsDecimal?.toString() ?: "2.10",
                    name2 = o2?.name ?: "Selección 2",
                    book2 = o2?.bookmaker ?: "Casa 2",
                    odds2 = o2?.oddsDecimal?.toString() ?: "2.05",
                    name3 = o3?.name ?: "Empate (X)",
                    book3 = o3?.bookmaker ?: "Casa 3",
                    odds3 = o3?.oddsDecimal?.toString() ?: "3.50"
                )
            )
        }
        computeCalculator()
    }

    fun updateCalculatorStake(stake: Double) {
        _uiState.update {
            it.copy(calculatorState = it.calculatorState.copy(totalStake = stake))
        }
        computeCalculator()
    }

    fun toggleThreeWay(isThreeWay: Boolean) {
        _uiState.update {
            it.copy(calculatorState = it.calculatorState.copy(isThreeWay = isThreeWay))
        }
        computeCalculator()
    }

    fun toggleOddsFormat(format: OddsFormat) {
        _uiState.update {
            it.copy(calculatorState = it.calculatorState.copy(oddsFormat = format))
        }
        computeCalculator()
    }

    fun toggleRoundStakes(round: Boolean) {
        _uiState.update {
            it.copy(calculatorState = it.calculatorState.copy(roundStakes = round))
        }
        computeCalculator()
    }

    fun updateCalcOutcome1(name: String, book: String, odds: String) {
        _uiState.update {
            it.copy(calculatorState = it.calculatorState.copy(name1 = name, book1 = book, odds1 = odds))
        }
        computeCalculator()
    }

    fun updateCalcOutcome2(name: String, book: String, odds: String) {
        _uiState.update {
            it.copy(calculatorState = it.calculatorState.copy(name2 = name, book2 = book, odds2 = odds))
        }
        computeCalculator()
    }

    fun updateCalcOutcome3(name: String, book: String, odds: String) {
        _uiState.update {
            it.copy(calculatorState = it.calculatorState.copy(name3 = name, book3 = book, odds3 = odds))
        }
        computeCalculator()
    }

    private fun parseOddsToDecimal(oddsStr: String, format: OddsFormat): Double {
        val cleaned = oddsStr.trim().replace(",", ".")
        val num = cleaned.toDoubleOrNull() ?: return 1.01
        return if (format == OddsFormat.AMERICAN) {
            ArbitrageMath.americanToDecimal(num.toInt())
        } else {
            if (num > 1.0) num else 1.01
        }
    }

    private fun computeCalculator() {
        val calc = _uiState.value.calculatorState
        val dec1 = parseOddsToDecimal(calc.odds1, calc.oddsFormat)
        val dec2 = parseOddsToDecimal(calc.odds2, calc.oddsFormat)

        val outcomes = mutableListOf(
            BetOutcome(calc.name1, calc.book1, dec1, ArbitrageMath.decimalToAmerican(dec1)),
            BetOutcome(calc.name2, calc.book2, dec2, ArbitrageMath.decimalToAmerican(dec2))
        )

        if (calc.isThreeWay) {
            val dec3 = parseOddsToDecimal(calc.odds3, calc.oddsFormat)
            outcomes.add(BetOutcome(calc.name3, calc.book3, dec3, ArbitrageMath.decimalToAmerican(dec3)))
        }

        val res = ArbitrageMath.calculateArbitrage(
            outcomes = outcomes,
            totalStake = calc.totalStake,
            roundStakes = calc.roundStakes
        )

        _uiState.update {
            it.copy(calculatorState = it.calculatorState.copy(result = res))
        }
    }

    // Promo conversion
    fun updatePromoAmount(amount: Double) {
        _uiState.update { it.copy(promoState = it.promoState.copy(promoAmount = amount)) }
        computePromo()
    }

    fun updatePromoOdds(promo: String, hedge: String) {
        _uiState.update { it.copy(promoState = it.promoState.copy(promoOdds = promo, hedgeOdds = hedge)) }
        computePromo()
    }

    private fun computePromo() {
        val promo = _uiState.value.promoState
        val pDec = promo.promoOdds.replace(",", ".").toDoubleOrNull() ?: 2.0
        val hDec = promo.hedgeOdds.replace(",", ".").toDoubleOrNull() ?: 1.8
        val res = ArbitrageMath.calculateFreeBetConversion(
            freeBetAmount = promo.promoAmount,
            promoDecimal = pDec,
            hedgeDecimal = hDec
        )
        _uiState.update {
            it.copy(promoState = it.promoState.copy(result = res))
        }
    }
}

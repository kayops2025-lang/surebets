package com.example.model

enum class OddsFormat {
    DECIMAL,
    AMERICAN
}

enum class SportType(val displayName: String, val iconName: String) {
    ALL("Todos", "emoji_events"),
    SOCCER("Fútbol", "sports_soccer"),
    BASKETBALL("Baloncesto", "sports_basketball"),
    TENNIS("Tenis", "sports_tennis"),
    BASEBALL("Béisbol", "sports_baseball"),
    FOOTBALL("NFL", "sports_football"),
    MMA("UFC / MMA", "sports_mma")
}

data class BetOutcome(
    val name: String,
    val bookmaker: String,
    val oddsDecimal: Double,
    val oddsAmerican: Int,
    val suggestedStake: Double = 0.0,
    val payoutIfWins: Double = 0.0
)

data class SurebetOpportunity(
    val id: String,
    val eventName: String,
    val sport: SportType,
    val commenceTime: String,
    val isThreeWay: Boolean,
    val outcomes: List<BetOutcome>,
    val profitPercentage: Double,
    val totalImpliedProbability: Double,
    val isLive: Boolean = false
)

data class CalculationResult(
    val totalStake: Double,
    val outcomes: List<BetOutcome>,
    val guaranteedPayout: Double,
    val netProfit: Double,
    val roiPercentage: Double,
    val isArbitrage: Boolean,
    val totalImpliedProb: Double
)

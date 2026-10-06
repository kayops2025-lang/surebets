package com.example.data

import com.example.model.BetOutcome
import com.example.model.SportType
import com.example.model.SurebetOpportunity
import com.example.util.ArbitrageMath

object MockOddsDataSource {

    val BOOKMAKERS = listOf(
        "Pinnacle",
        "Bet365",
        "FanDuel",
        "DraftKings",
        "BetMGM",
        "Caesars",
        "BetOnline",
        "Bwin",
        "Betfair"
    )

    fun getInitialSurebets(): List<SurebetOpportunity> {
        return listOf(
            createOpportunity(
                id = "arb_1",
                eventName = "Real Madrid vs Manchester City",
                sport = SportType.SOCCER,
                commenceTime = "Hoy 21:00 CET",
                isThreeWay = true,
                outcomes = listOf(
                    BetOutcome("Real Madrid", "Pinnacle", 3.10, ArbitrageMath.decimalToAmerican(3.10)),
                    BetOutcome("Empate (X)", "Betfair", 3.65, ArbitrageMath.decimalToAmerican(3.65)),
                    BetOutcome("Manchester City", "FanDuel", 2.62, ArbitrageMath.decimalToAmerican(2.62))
                )
            ),
            createOpportunity(
                id = "arb_2",
                eventName = "Boston Celtics vs Los Angeles Lakers",
                sport = SportType.BASKETBALL,
                commenceTime = "Hoy 19:30 EST",
                isThreeWay = false,
                outcomes = listOf(
                    BetOutcome("Boston Celtics", "DraftKings", 1.95, ArbitrageMath.decimalToAmerican(1.95)),
                    BetOutcome("Los Angeles Lakers", "Pinnacle", 2.24, ArbitrageMath.decimalToAmerican(2.24))
                )
            ),
            createOpportunity(
                id = "arb_3",
                eventName = "Carlos Alcaraz vs Jannik Sinner",
                sport = SportType.TENNIS,
                commenceTime = "Mañana 14:00 CET",
                isThreeWay = false,
                outcomes = listOf(
                    BetOutcome("Carlos Alcaraz", "Bet365", 2.12, ArbitrageMath.decimalToAmerican(2.12)),
                    BetOutcome("Jannik Sinner", "BetMGM", 2.05, ArbitrageMath.decimalToAmerican(2.05))
                )
            ),
            createOpportunity(
                id = "arb_4",
                eventName = "Kansas City Chiefs vs San Francisco 49ers",
                sport = SportType.FOOTBALL,
                commenceTime = "Domingo 20:20 EST",
                isThreeWay = false,
                outcomes = listOf(
                    BetOutcome("Chiefs ML", "Caesars", 2.15, ArbitrageMath.decimalToAmerican(2.15)),
                    BetOutcome("49ers ML", "FanDuel", 2.04, ArbitrageMath.decimalToAmerican(2.04))
                )
            ),
            createOpportunity(
                id = "arb_5",
                eventName = "FC Barcelona vs Paris Saint-Germain",
                sport = SportType.SOCCER,
                commenceTime = "Mañana 21:00 CET",
                isThreeWay = true,
                outcomes = listOf(
                    BetOutcome("Barcelona", "BetOnline", 2.80, ArbitrageMath.decimalToAmerican(2.80)),
                    BetOutcome("Empate (X)", "Pinnacle", 3.80, ArbitrageMath.decimalToAmerican(3.80)),
                    BetOutcome("PSG", "DraftKings", 2.95, ArbitrageMath.decimalToAmerican(2.95))
                )
            ),
            createOpportunity(
                id = "arb_6",
                eventName = "New York Yankees vs Los Angeles Dodgers",
                sport = SportType.BASEBALL,
                commenceTime = "Hoy 22:05 EST",
                isThreeWay = false,
                outcomes = listOf(
                    BetOutcome("NY Yankees", "FanDuel", 2.20, ArbitrageMath.decimalToAmerican(2.20)),
                    BetOutcome("LA Dodgers", "Bet365", 1.98, ArbitrageMath.decimalToAmerican(1.98))
                )
            ),
            createOpportunity(
                id = "arb_7",
                eventName = "Ilia Topuria vs Alexander Volkanovski",
                sport = SportType.MMA,
                commenceTime = "Sábado 23:00 EST",
                isThreeWay = false,
                outcomes = listOf(
                    BetOutcome("Topuria", "Pinnacle", 1.92, ArbitrageMath.decimalToAmerican(1.92)),
                    BetOutcome("Volkanovski", "BetMGM", 2.30, ArbitrageMath.decimalToAmerican(2.30))
                )
            )
        )
    }

    private fun createOpportunity(
        id: String,
        eventName: String,
        sport: SportType,
        commenceTime: String,
        isThreeWay: Boolean,
        outcomes: List<BetOutcome>
    ): SurebetOpportunity {
        val invSum = outcomes.sumOf { 1.0 / it.oddsDecimal }
        val profitRoi = if (invSum < 1.0) ((1.0 / invSum) - 1.0) * 100.0 else (1.0 - invSum) * 100.0
        return SurebetOpportunity(
            id = id,
            eventName = eventName,
            sport = sport,
            commenceTime = commenceTime,
            isThreeWay = isThreeWay,
            outcomes = outcomes,
            profitPercentage = (profitRoi * 100).toInt() / 100.0,
            totalImpliedProbability = (invSum * 1000).toInt() / 10.0,
            isLive = true
        )
    }
}

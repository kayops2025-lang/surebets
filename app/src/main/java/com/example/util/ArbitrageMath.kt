package com.example.util

import com.example.model.BetOutcome
import com.example.model.CalculationResult
import kotlin.math.abs
import kotlin.math.roundToInt

object ArbitrageMath {

    fun americanToDecimal(american: Int): Double {
        return if (american > 0) {
            1.0 + (american.toDouble() / 100.0)
        } else {
            1.0 + (100.0 / abs(american.toDouble()))
        }
    }

    fun decimalToAmerican(decimal: Double): Int {
        if (decimal <= 1.0) return 0
        return if (decimal >= 2.0) {
            ((decimal - 1.0) * 100.0).roundToInt()
        } else {
            -((100.0 / (decimal - 1.0))).roundToInt()
        }
    }

    fun calculateArbitrage(
        outcomes: List<BetOutcome>,
        totalStake: Double,
        roundStakes: Boolean = false
    ): CalculationResult {
        if (outcomes.isEmpty() || totalStake <= 0.0) {
            return CalculationResult(
                totalStake = totalStake,
                outcomes = outcomes,
                guaranteedPayout = 0.0,
                netProfit = 0.0,
                roiPercentage = 0.0,
                isArbitrage = false,
                totalImpliedProb = 0.0
            )
        }

        // Inverted sum = sum(1 / decimal_i)
        val invertedOdds = outcomes.map { 1.0 / it.oddsDecimal }
        val totalInv = invertedOdds.sum()
        val isArb = totalInv < 1.0 && totalInv > 0.0

        val computedOutcomes = outcomes.mapIndexed { index, outcome ->
            val rawStake = (totalStake * invertedOdds[index]) / totalInv
            val finalStake = if (roundStakes) {
                // Round to nearest integer (or 2 decimal places if stake is small)
                if (totalStake >= 50) rawStake.roundToInt().toDouble() else (rawStake * 10).roundToInt() / 10.0
            } else {
                (rawStake * 100.0).roundToInt() / 100.0
            }
            val payout = finalStake * outcome.oddsDecimal
            outcome.copy(
                suggestedStake = finalStake,
                payoutIfWins = (payout * 100.0).roundToInt() / 100.0
            )
        }

        val actualTotalStake = computedOutcomes.sumOf { it.suggestedStake }
        val minPayout = computedOutcomes.minOfOrNull { it.payoutIfWins } ?: 0.0
        val netProfit = minPayout - actualTotalStake
        val roi = if (actualTotalStake > 0) (netProfit / actualTotalStake) * 100.0 else 0.0

        return CalculationResult(
            totalStake = actualTotalStake,
            outcomes = computedOutcomes,
            guaranteedPayout = minPayout,
            netProfit = netProfit,
            roiPercentage = roi,
            isArbitrage = isArb,
            totalImpliedProb = totalInv * 100.0
        )
    }

    /**
     * Calculates optimal hedge for a Free Bet (SNR - Stake Not Returned)
     */
    fun calculateFreeBetConversion(
        freeBetAmount: Double,
        promoDecimal: Double,
        hedgeDecimal: Double
    ): FreeBetResult {
        if (freeBetAmount <= 0.0 || promoDecimal <= 1.0 || hedgeDecimal <= 1.0) {
            return FreeBetResult(0.0, 0.0, 0.0, 0.0)
        }
        val hedgeStake = (freeBetAmount * (promoDecimal - 1.0)) / hedgeDecimal
        val payoutIfPromoWins = (freeBetAmount * (promoDecimal - 1.0)) - hedgeStake
        val payoutIfHedgeWins = hedgeStake * (hedgeDecimal - 1.0)
        val lockedProfit = minOf(payoutIfPromoWins, payoutIfHedgeWins)
        val conversionRate = (lockedProfit / freeBetAmount) * 100.0

        return FreeBetResult(
            suggestedHedgeStake = (hedgeStake * 100).roundToInt() / 100.0,
            guaranteedProfit = (lockedProfit * 100).roundToInt() / 100.0,
            conversionRate = (conversionRate * 10).roundToInt() / 10.0,
            payoutIfHedgeWins = (payoutIfHedgeWins * 100).roundToInt() / 100.0
        )
    }
}

data class FreeBetResult(
    val suggestedHedgeStake: Double,
    val guaranteedProfit: Double,
    val conversionRate: Double,
    val payoutIfHedgeWins: Double
)

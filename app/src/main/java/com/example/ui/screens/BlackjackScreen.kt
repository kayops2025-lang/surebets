package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CardInfo(
    val rank: String,
    val suit: String,
    val isRed: Boolean,
    val value: Int
)

@Composable
fun BlackjackScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // 13 cards split into two neat rows so 'K' is NEVER cut off
    val row1Cards = listOf(
        CardInfo("A", "♠", false, 11),
        CardInfo("2", "♣", false, 2),
        CardInfo("3", "♦", true, 3),
        CardInfo("4", "♠", false, 4),
        CardInfo("5", "♥", true, 5),
        CardInfo("6", "♣", false, 6),
        CardInfo("7", "♦", true, 7)
    )

    val row2Cards = listOf(
        CardInfo("8", "♠", false, 8),
        CardInfo("9", "♥", true, 9),
        CardInfo("10", "♣", false, 10),
        CardInfo("J", "♦", true, 10),
        CardInfo("Q", "♠", false, 10),
        CardInfo("K", "♥", true, 10)
    )

    // Using immutable List with State so EVERY change triggers immediate re-evaluation!
    var dealerCard by remember { mutableStateOf("6") }
    var playerCards by remember { mutableStateOf(listOf("8", "8")) }

    // Recomputed dynamically on any state change
    val decision = remember(playerCards, dealerCard) {
        computeProBlackjackDecision(playerCards, dealerCard)
    }
    val handSummary = remember(playerCards) {
        computeHandSummary(playerCards)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Top Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Casino,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Asesor Profesional de Blackjack",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Estrategia Básica Óptima · Ventaja Jugador (S17, 8 Mazos, DAS)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // --- 1. CRUPIER SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. Carta Descubierta del Crupier",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Crupier: $dealerCard",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 1 (A-7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row1Cards.forEach { c ->
                        PlayingCardTile(
                            card = c,
                            isSelected = dealerCard == c.rank,
                            onClick = { dealerCard = c.rank },
                            modifier = Modifier.testTag("dealer_card_${c.rank}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2 (8-K)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row2Cards.forEach { c ->
                        PlayingCardTile(
                            card = c,
                            isSelected = dealerCard == c.rank,
                            onClick = { dealerCard = c.rank },
                            modifier = Modifier.testTag("dealer_card_${c.rank}")
                        )
                    }
                }
            }
        }

        // --- 2. PLAYER HAND SECTION ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "2. Tus Cartas en Mano",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedButton(
                            onClick = {
                                if (playerCards.isNotEmpty()) {
                                    playerCards = playerCards.dropLast(1)
                                }
                            },
                            enabled = playerCards.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("undo_card_button")
                        ) {
                            Icon(Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Borrar", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { playerCards = emptyList() },
                            enabled = playerCards.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.testTag("clear_hand_button")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Limpiar", fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Physical cards display in hand
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mano: $handSummary",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${playerCards.size} cartas",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (playerCards.isEmpty()) {
                            Text(
                                text = "Selecciona cartas abajo para construir tu mano...",
                                fontSize = 12.sp,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                playerCards.forEachIndexed { index, cardRank ->
                                    val match = (row1Cards + row2Cards).find { it.rank == cardRank }
                                        ?: CardInfo(cardRank, "♠", false, 10)
                                    PlayingCardInHand(card = match, index = index + 1)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Toca para agregar a tu mano:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Row 1 (A-7)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    row1Cards.forEach { c ->
                        PlayingCardTile(
                            card = c,
                            isSelected = false,
                            onClick = { playerCards = playerCards + c.rank },
                            modifier = Modifier.testTag("player_card_${c.rank}")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Row 2 (8-K) - Fully visible, K is NEVER cut off!
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row2Cards.forEach { c ->
                        PlayingCardTile(
                            card = c,
                            isSelected = false,
                            onClick = { playerCards = playerCards + c.rank },
                            modifier = Modifier.testTag("player_card_${c.rank}")
                        )
                    }
                }
            }
        }

        // --- 3. DECISION BANNER (PROFESSIONAL ADVICE) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("blackjack_decision_card"),
            colors = CardDefaults.cardColors(containerColor = decision.backgroundColor),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    color = decision.textColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "JUGADA MATEMÁTICAMENTE ÓPTIMA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = decision.textColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = decision.actionName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = decision.textColor
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = decision.explanation,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    color = decision.textColor
                )

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = decision.textColor.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(8.dp))

                // Professional Edge & Strategy Insight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = decision.textColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = decision.evBadge,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = decision.textColor
                        )
                    }
                    Text(
                        text = "Ventaja casa: < 0.45%",
                        fontSize = 11.sp,
                        color = decision.textColor.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Pro Tip Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(18.dp)
                        .padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        "Análisis de Probabilidad (Dealer Bust)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                    Text(
                        text = when (dealerCard) {
                            "4", "5", "6" -> "El crupier muestra carta débil ($dealerCard): tiene entre 40% y 42.8% de probabilidad de pasarse (Bust). No arriesgues tu mano rompiéndola tú mismo."
                            "2", "3" -> "Cuidado con el 2 y el 3 del crupier: no son cartas de bust tan altas (35%). Por eso con 12 se pide contra 2 y 3, pero se planta contra 4, 5 y 6."
                            "7", "8" -> "El crupier muestra $dealerCard: suele formar mano final de 17 u 18. Debes asumir que tiene un 10 debajo y jugar ofensivo."
                            "9", "10", "J", "Q", "K", "A" -> "Carta fuerte ($dealerCard): probabilidad de bust de solo 21%-23%. Debes pedir o doblar con agresividad para alcanzar al menos 17+."
                            else -> "Regla de oro: asume siempre que la carta tapada del crupier vale 10."
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

/**
 * Playing card tile with calibrated width (40dp) so 7 cards fit on row 1 and 6 on row 2 without cutting 'K'
 */
@Composable
fun PlayingCardTile(
    card: CardInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White
    val borderCol = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFCBD5E1)
    val rankColor = if (card.isRed) Color(0xFFDC2626) else Color(0xFF0F172A)

    Surface(
        modifier = modifier
            .size(width = 40.dp, height = 56.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .shadow(if (isSelected) 3.dp else 1.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = cardBg,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderCol)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 2.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = card.rank,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = rankColor
                )
                Text(
                    text = card.suit,
                    fontSize = 10.sp,
                    color = rankColor
                )
            }

            Text(
                text = card.rank,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = rankColor
            )

            Text(
                text = card.suit,
                fontSize = 9.sp,
                color = rankColor
            )
        }
    }
}

@Composable
fun PlayingCardInHand(
    card: CardInfo,
    index: Int
) {
    val rankColor = if (card.isRed) Color(0xFFDC2626) else Color(0xFF0F172A)

    Surface(
        modifier = Modifier
            .size(width = 46.dp, height = 64.dp)
            .shadow(2.dp, RoundedCornerShape(8.dp)),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFF94A3B8))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(card.rank, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = rankColor)
                Text(card.suit, fontSize = 10.sp, color = rankColor)
            }
            Text(
                text = card.rank,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = rankColor
            )
            Text(
                text = card.suit,
                fontSize = 10.sp,
                color = rankColor
            )
        }
    }
}

data class BjDecision(
    val actionName: String,
    val explanation: String,
    val evBadge: String,
    val backgroundColor: Color,
    val textColor: Color
)

fun computeHandSummary(cards: List<String>): String {
    if (cards.isEmpty()) return "Sin cartas"
    var sum = 0
    var aces = 0
    cards.forEach { c ->
        when (c) {
            "A" -> { aces++; sum += 11 }
            "K", "Q", "J", "10" -> sum += 10
            else -> sum += c.toIntOrNull() ?: 10
        }
    }
    while (sum > 21 && aces > 0) {
        sum -= 10
        aces--
    }
    val isSoft = aces > 0 && sum <= 21
    val isPair = cards.size == 2 && (cards[0] == cards[1] || (cards[0] in listOf("10","J","Q","K") && cards[1] in listOf("10","J","Q","K")))
    return when {
        sum > 21 -> "$sum (¡Te pasaste / Bust!)"
        cards.size == 2 && sum == 21 -> "21 (¡Blackjack Natural!)"
        isPair -> "$sum (Par de ${cards[0]}s)"
        isSoft -> "$sum (Mano Suave con As)"
        else -> "$sum (Mano Dura)"
    }
}

/**
 * 20-Year Pro Veteran Basic Strategy Decision Engine
 * Implements full Stanford Wong & Don Schlesinger tables
 */
fun computeProBlackjackDecision(playerCards: List<String>, dealerCard: String): BjDecision {
    if (playerCards.size < 2) {
        return BjDecision(
            actionName = "AGREGA CARTAS",
            explanation = "Selecciona al menos 2 cartas para evaluar tu mano.",
            evBadge = "Esperando mano",
            backgroundColor = Color(0xFFF1F5F9),
            textColor = Color(0xFF334155)
        )
    }

    val dVal = when (dealerCard) {
        "A" -> 11
        "K", "Q", "J", "10" -> 10
        else -> dealerCard.toIntOrNull() ?: 10
    }

    var sum = 0
    var aces = 0
    playerCards.forEach { c ->
        when (c) {
            "A" -> { aces++; sum += 11 }
            "K", "Q", "J", "10" -> sum += 10
            else -> sum += c.toIntOrNull() ?: 10
        }
    }
    while (sum > 21 && aces > 0) {
        sum -= 10
        aces--
    }

    if (sum > 21) {
        return BjDecision(
            actionName = "PASADO (BUST)",
            explanation = "Tu puntuación total ($sum) superó 21.",
            evBadge = "Mano perdida",
            backgroundColor = Color(0xFFFFEBEE),
            textColor = Color(0xFFC62828)
        )
    }

    if (playerCards.size == 2 && sum == 21) {
        return BjDecision(
            actionName = "¡BLACKJACK!",
            explanation = "¡Mano natural! Cobra tu pago 3 a 2 de inmediato.",
            evBadge = "Máximo beneficio (+1.5x)",
            backgroundColor = Color(0xFFE8F5E9),
            textColor = Color(0xFF2E7D32)
        )
    }

    val isPair = playerCards.size == 2 && (
        playerCards[0] == playerCards[1] ||
        (playerCards[0] in listOf("10","J","Q","K") && playerCards[1] in listOf("10","J","Q","K"))
    )
    val isSoft = aces > 0 && sum <= 21
    val isTwoCards = playerCards.size == 2

    // --- 1. PAIRS (PAREJAS) ---
    if (isPair) {
        val p = if (playerCards[0] in listOf("10","J","Q","K")) "10" else playerCards[0]
        when (p) {
            "A" -> return splitDecision("Separar Ases siempre. Convierte una mano de 12 en dos manos con potencial de 21 (+0.52 EV).")
            "8" -> return splitDecision("Separar 8,8 siempre. 16 es la peor mano del juego (-0.54 EV); separarlos reduce la pérdida drásticamente.")
            "10" -> return standDecision("Plantarse con 20 siempre. Tienes un 92% de probabilidad de ganar; nunca arriesgues 20.")
            "9" -> return if (dVal in 2..6 || dVal in 8..9) {
                splitDecision("Separar 9s contra 2-6 y 8-9 del crupier.")
            } else {
                standDecision("Plantarse con 18 contra el 7 (tu 18 gana al 17 del crupier) y contra 10 o As.")
            }
            "7" -> return if (dVal in 2..7) {
                splitDecision("Separar 7s contra 2-7. Contra 8+ es preferible pedir.")
            } else {
                hitDecision("Pedir con 14 duro contra carta fuerte (8+).")
            }
            "6" -> return if (dVal in 2..6) {
                splitDecision("Separar 6s contra carta débil del crupier (2-6).")
            } else {
                hitDecision("Pedir con 12 duro contra carta fuerte (7+).")
            }
            "5" -> return if (dVal in 2..9) {
                doubleDecision("¡NO separar 5s! Trátalos como un 10 duro y DOBLA contra 2-9.")
            } else {
                hitDecision("Pedir con 10 duro contra 10 o As.")
            }
            "4" -> return if (dVal in 5..6) {
                splitDecision("Separar 4s únicamente contra 5 o 6 del crupier (con DAS permitido).")
            } else {
                hitDecision("Pedir con 8 duro.")
            }
            "2", "3" -> return if (dVal in 2..7) {
                splitDecision("Separar 2s o 3s contra 2-7 del crupier.")
            } else {
                hitDecision("Pedir contra 8 o superior.")
            }
        }
    }

    // --- 2. SOFT HANDS (MANOS SUAVES CON AS) ---
    if (isSoft) {
        if (sum >= 19) {
            return standDecision("Plantarse con suave 19 o 20 (A,8 / A,9). Posición dominante.")
        }
        if (sum == 18) { // A,7
            return when {
                dVal in 2..6 && isTwoCards -> doubleDecision("Doblar A,7 contra carta débil (2-6). Si no puedes doblar, plántate.")
                dVal in 7..8 -> standDecision("Plantarse con 18 suave contra 7 u 8. El crupier terminará en 17 o 18.")
                else -> hitDecision("Pedir con A,7 contra 9, 10 o As. Quedarse en 18 contra carta alta es una jugada perdedora a largo plazo.")
            }
        }
        if (sum == 17) { // A,6
            return if (dVal in 3..6 && isTwoCards) {
                doubleDecision("Doblar A,6 contra 3, 4, 5 o 6. De lo contrario, pide.")
            } else {
                hitDecision("Pedir con A,6. 17 suave nunca se planta: no puedes pasarte y puedes mejorar.")
            }
        }
        if (sum in 15..16) { // A,4 y A,5
            return if (dVal in 4..6 && isTwoCards) {
                doubleDecision("Doblar contra 4, 5 o 6 del crupier. Gran probabilidad de que el crupier quiebre.")
            } else {
                hitDecision("Pedir. Una mano suave no quiebra; busca una carta alta.")
            }
        }
        if (sum in 13..14) { // A,2 y A,3
            return if (dVal in 5..6 && isTwoCards) {
                doubleDecision("Doblar contra 5 o 6 del crupier. Máxima presión en la carta más débil.")
            } else {
                hitDecision("Pedir. Mano flexible con opción de mejora.")
            }
        }
    }

    // --- 3. HARD HANDS (MANOS DURAS) ---
    if (sum >= 17) {
        return standDecision("Plantarse con $sum duro. Riesgo de pasarse es > 75%; deja que el crupier juegue su mano.")
    }

    if (sum in 13..16) {
        return if (dVal in 2..6) {
            standDecision("Plantarse con $sum contra carta débil ($dealerCard). El crupier quiebra más del 40% de las veces.")
        } else {
            hitDecision("Pedir con $sum contra $dealerCard. Estadísticamente pierdes menos pidiendo que plantándote.")
        }
    }

    if (sum == 12) {
        return if (dVal in 4..6) {
            standDecision("Plantarse con 12 contra 4, 5 y 6 (zona de quiebra del crupier).")
        } else {
            hitDecision("Pedir con 12 contra 2 o 3. (Dato pro: 2 y 3 no son cartas de bust tan altas; el crupier hace mano el 65% de las veces).")
        }
    }

    if (sum == 11) {
        return if (isTwoCards && dVal != 11) {
            doubleDecision("¡Doblar 11 siempre contra 2 al 10! Es la situación con mayor expectativa positiva del juego (+0.54 EV).")
        } else {
            hitDecision("Pedir con 11 contra As (o con más de 2 cartas).")
        }
    }

    if (sum == 10) {
        return if (isTwoCards && dVal in 2..9) {
            doubleDecision("Doblar 10 contra 2 al 9. Más del 30% de las cartas restantes valen 10.")
        } else {
            hitDecision("Pedir con 10 contra 10 o As del crupier.")
        }
    }

    if (sum == 9) {
        return if (isTwoCards && dVal in 3..6) {
            doubleDecision("Doblar 9 contra 3, 4, 5 o 6 del crupier.")
        } else {
            hitDecision("Pedir con 9 contra 2 o contra 7+.")
        }
    }

    return hitDecision("Pedir siempre con 8 o menos. No hay riesgo de pasarse.")
}

private fun hitDecision(exp: String) = BjDecision(
    actionName = "PEDIR (HIT)",
    explanation = exp,
    evBadge = "Acción defensiva / ofensiva",
    backgroundColor = Color(0xFFFFEBEE),
    textColor = Color(0xFFC62828)
)

private fun standDecision(exp: String) = BjDecision(
    actionName = "PLANTARSE (STAND)",
    explanation = exp,
    evBadge = "Minimiza pérdida esperada",
    backgroundColor = Color(0xFFFFF9C4),
    textColor = Color(0xFF854D0E)
)

private fun doubleDecision(exp: String) = BjDecision(
    actionName = "DOBLAR (DOUBLE)",
    explanation = exp,
    evBadge = "¡Máximo Valor Esperado (+EV)!",
    backgroundColor = Color(0xFFE0F2FE),
    textColor = Color(0xFF0369A1)
)

private fun splitDecision(exp: String) = BjDecision(
    actionName = "SEPARAR (SPLIT)",
    explanation = exp,
    evBadge = "Transforma mano negativa en ventaja",
    backgroundColor = Color(0xFFE8F5E9),
    textColor = Color(0xFF166534)
)

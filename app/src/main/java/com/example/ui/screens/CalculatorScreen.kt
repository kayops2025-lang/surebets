package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OddsFormat
import com.example.ui.CalculatorUiState

@Composable
fun CalculatorScreen(
    calcState: CalculatorUiState,
    onStakeChange: (Double) -> Unit,
    onThreeWayToggle: (Boolean) -> Unit,
    onOddsFormatToggle: (OddsFormat) -> Unit,
    onRoundStakesToggle: (Boolean) -> Unit,
    onOutcome1Change: (String, String, String) -> Unit,
    onOutcome2Change: (String, String, String) -> Unit,
    onOutcome3Change: (String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Title
        Text(
            text = "Calculadora de Surebets",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Capital / Stake Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Capital Total (Stake)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = if (calcState.totalStake > 0) calcState.totalStake.toString() else "",
                    onValueChange = { input ->
                        val clean = input.replace(",", ".").toDoubleOrNull() ?: 0.0
                        onStakeChange(clean)
                    },
                    prefix = { Text("$ ") },
                    placeholder = { Text("100.0") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("stake_input"),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Stakes Chips
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(50.0, 100.0, 250.0, 500.0, 1000.0).forEach { amount ->
                        FilterChip(
                            selected = calcState.totalStake == amount,
                            onClick = { onStakeChange(amount) },
                            label = { Text("$${amount.toInt()}") },
                            modifier = Modifier.testTag("quick_stake_${amount.toInt()}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }
        }

        // Configuration Toggles
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo 3 Vías (1X2 Fútbol)", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Incluye opción de Empate además de Victoria Local / Visitante",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = calcState.isThreeWay,
                        onCheckedChange = onThreeWayToggle,
                        modifier = Modifier.testTag("three_way_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Redondeo Inteligente", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Evita decimales extraños (ej. $37.42) que alertan a las casas de apuestas",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                    Switch(
                        checked = calcState.roundStakes,
                        onCheckedChange = onRoundStakesToggle,
                        modifier = Modifier.testTag("round_stakes_switch")
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Formato de Cuotas", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(
                            selected = calcState.oddsFormat == OddsFormat.DECIMAL,
                            onClick = { onOddsFormatToggle(OddsFormat.DECIMAL) },
                            label = { Text("Decimal (2.10)") }
                        )
                        FilterChip(
                            selected = calcState.oddsFormat == OddsFormat.AMERICAN,
                            onClick = { onOddsFormatToggle(OddsFormat.AMERICAN) },
                            label = { Text("Americano (+110)") }
                        )
                    }
                }
            }
        }

        // Odds Inputs
        Text(
            text = "Cuotas y Casas de Apuestas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        // Outcome 1
        OutcomeInputCard(
            label = "Selección 1 (ej. Local)",
            name = calcState.name1,
            book = calcState.book1,
            odds = calcState.odds1,
            onUpdate = { n, b, o -> onOutcome1Change(n, b, o) },
            tagPrefix = "outcome1"
        )

        // Outcome 2
        OutcomeInputCard(
            label = "Selección 2 (ej. Visitante)",
            name = calcState.name2,
            book = calcState.book2,
            odds = calcState.odds2,
            onUpdate = { n, b, o -> onOutcome2Change(n, b, o) },
            tagPrefix = "outcome2"
        )

        // Outcome 3 (if 3-way)
        if (calcState.isThreeWay) {
            OutcomeInputCard(
                label = "Selección 3 (Empate X)",
                name = calcState.name3,
                book = calcState.book3,
                odds = calcState.odds3,
                onUpdate = { n, b, o -> onOutcome3Change(n, b, o) },
                tagPrefix = "outcome3"
            )
        }

        // Calculation Results Card
        val result = calcState.result
        if (result != null) {
            val isArb = result.isArbitrage
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("calculation_result_card"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isArb) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isArb) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isArb) Color(0xFF2E7D32) else Color(0xFFC62828),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArb) "¡Surebet Encontrada! Arbitraje Positivo" else "Sin Arbitraje (Margen de la Casa)",
                            fontWeight = FontWeight.Bold,
                            color = if (isArb) Color(0xFF2E7D32) else Color(0xFFC62828),
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isArb) {
                            "Ganancia asegurada de $${"%.2f".format(result.netProfit)} (${"%.2f".format(result.roiPercentage)}% ROI) independientemente de qué equipo gane."
                        } else {
                            "La suma de probabilidades es ${"%.1f".format(result.totalImpliedProb)}% (> 100%). Perderías aproximadamente $${"%.2f".format(-result.netProfit)} por el margen de las casas."
                        },
                        fontSize = 13.sp,
                        color = Color.Black.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Distribución de Apuestas Recomendada:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    result.outcomes.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = item.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "${item.bookmaker} @ ${item.oddsDecimal}",
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Apostar: $${"%.2f".format(item.suggestedStake)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isArb) Color(0xFF1B5E20) else Color.Black
                                    )
                                    Text(
                                        text = "Retorno: $${"%.2f".format(item.payoutIfWins)}",
                                        fontSize = 11.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Inversión Total: $${"%.2f".format(result.totalStake)}",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "Cobro Garantizado: $${"%.2f".format(result.guaranteedPayout)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isArb) Color(0xFF2E7D32) else Color.Black
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun OutcomeInputCard(
    label: String,
    name: String,
    book: String,
    odds: String,
    onUpdate: (String, String, String) -> Unit,
    tagPrefix: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { onUpdate(it, book, odds) },
                    label = { Text("Selección") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.2f)
                        .testTag("${tagPrefix}_name"),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = book,
                    onValueChange = { onUpdate(name, it, odds) },
                    label = { Text("Casa") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1.0f)
                        .testTag("${tagPrefix}_book"),
                    shape = RoundedCornerShape(8.dp)
                )

                OutlinedTextField(
                    value = odds,
                    onValueChange = { onUpdate(name, book, it) },
                    label = { Text("Cuota") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(0.9f)
                        .testTag("${tagPrefix}_odds"),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}

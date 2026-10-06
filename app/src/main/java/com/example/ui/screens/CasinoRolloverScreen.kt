package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CasinoRolloverScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var bonoAmount by remember { mutableDoubleStateOf(150.0) }
    var depositoAmount by remember { mutableDoubleStateOf(0.0) }
    var rolloverMultiplier by remember { mutableDoubleStateOf(1.0) } // 1x
    var contributionPct by remember { mutableDoubleStateOf(100.0) }
    var slotRtp by remember { mutableDoubleStateOf(96.0) } // 96%
    var betPerSpin by remember { mutableDoubleStateOf(0.20) }

    // Math calculations
    val requirement = (bonoAmount + depositoAmount) * rolloverMultiplier
    val effectiveTurnover = if (contributionPct > 0) requirement / (contributionPct / 100.0) else 0.0
    val spinsNeeded = if (betPerSpin > 0) effectiveTurnover / betPerSpin else 0.0
    val houseEdge = (100.0 - slotRtp) / 100.0
    val expectedLoss = effectiveTurnover * houseEdge
    val netEv = bonoAmount - expectedLoss
    val retentionPct = if (bonoAmount > 0) (netEv / bonoAmount) * 100.0 else 0.0
    val totalSeconds = spinsNeeded * 3.0 // ~3 seconds per spin
    val hoursEstimated = totalSeconds / 3600.0
    val hourlyProfit = if (hoursEstimated > 0) netEv / hoursEstimated else 0.0

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header
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
                    text = "Simulador de Rollover & Casino",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Calcula el Valor Esperado (+EV) y horas para liberar bonos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Offer Parameters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("1. Términos del Bono de Casino", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = if (bonoAmount > 0) bonoAmount.toString() else "",
                        onValueChange = { bonoAmount = it.replace(",", ".").toDoubleOrNull() ?: 0.0 },
                        label = { Text("Bono ($)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("casino_bonus_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = if (depositoAmount > 0) depositoAmount.toString() else "0",
                        onValueChange = { depositoAmount = it.replace(",", ".").toDoubleOrNull() ?: 0.0 },
                        label = { Text("Depósito ($)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("casino_deposit_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = rolloverMultiplier.toString(),
                        onValueChange = { rolloverMultiplier = it.replace(",", ".").toDoubleOrNull() ?: 1.0 },
                        label = { Text("Rollover (veces)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("casino_rollover_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = contributionPct.toString(),
                        onValueChange = { contributionPct = it.replace(",", ".").toDoubleOrNull() ?: 100.0 },
                        label = { Text("Contribución (%)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("casino_contrib_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }
        }

        // Slot Game Parameters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(14.dp),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("2. Configuración de la Slot / Juego", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = slotRtp.toString(),
                        onValueChange = { slotRtp = it.replace(",", ".").toDoubleOrNull() ?: 96.0 },
                        label = { Text("RTP de la Slot (%)") },
                        placeholder = { Text("ej. 96.0%") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("casino_rtp_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                    OutlinedTextField(
                        value = betPerSpin.toString(),
                        onValueChange = { betPerSpin = it.replace(",", ".").toDoubleOrNull() ?: 0.20 },
                        label = { Text("Apuesta por Giro ($)") },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("casino_bet_spin_input"),
                        shape = RoundedCornerShape(8.dp)
                    )
                }
                Text(
                    "Slots recomendadas con alto RTP: Sweet Bonanza (96.48%), Coffee Explosion (96.52%), Huff N Puff (96.0%).",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Results Card
        val isPositiveEv = netEv > 0.0
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("casino_result_card"),
            colors = CardDefaults.cardColors(
                containerColor = if (isPositiveEv) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isPositiveEv) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isPositiveEv) Color(0xFF2E7D32) else Color(0xFFC62828),
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPositiveEv) "¡Promoción +EV Rentable!" else "Promoción -EV (Ventaja del Casino)",
                        fontWeight = FontWeight.Bold,
                        color = if (isPositiveEv) Color(0xFF2E7D32) else Color(0xFFC62828),
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(10.dp))

                // Breakdown list
                ResultRow(label = "Requisito total a apostar", value = "$${"%.2f".format(effectiveTurnover)}")
                ResultRow(label = "Giros aproximados necesarios", value = "${spinsNeeded.toInt()} spins")
                ResultRow(label = "Costo estadístico del juego (Edge)", value = "-$${"%.2f".format(expectedLoss)}")
                ResultRow(
                    label = "Valor Esperado Neto (EV)",
                    value = "$${"%.2f".format(netEv)}",
                    isHighlight = true,
                    highlightColor = if (isPositiveEv) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )
                ResultRow(label = "Retención neta del bono", value = "${"%.1f".format(retentionPct)}%")
                ResultRow(label = "Tiempo estimado (@ 3s/giro)", value = "${"%.1f".format(hoursEstimated)} horas")
                ResultRow(label = "Ganancia estimada por hora", value = "$${"%.2f".format(hourlyProfit)}/hora")
            }
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    highlightColor: Color = Color.Black
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = Color.DarkGray)
        Text(
            text = value,
            fontSize = if (isHighlight) 16.sp else 13.sp,
            fontWeight = if (isHighlight) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = highlightColor
        )
    }
}

package com.example.herretols.ui.admin

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun BarChartCustom(data: List<Pair<String, Int>>) {
    val maxVal = data.maxOfOrNull { it.second } ?: 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        data.forEach { item ->
            // Calcula el ancho porcentual de la barra de forma dinámica
            val progress = item.second.toFloat() / maxVal.toFloat()
            val animatedProgress by animateFloatAsState(targetValue = progress)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nombre del producto
                Text(
                    text = item.first,
                    modifier = Modifier.weight(0.4f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Contenedor de la barra analítica
                Box(
                    modifier = Modifier
                        .weight(0.5f)
                        .height(20.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress) // Escala visual reactiva
                            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    )
                }

                // Cantidad vendida al extremo derecho
                Text(
                    text = "${item.second} und.",
                    modifier = Modifier
                        .weight(0.15f)
                        .padding(start = 4.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
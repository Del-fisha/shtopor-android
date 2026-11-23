package com.company.shtopor.vzvod.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.company.shtopor.vzvod.ui.components.SoftPanel
import com.company.shtopor.vzvod.ui.theme.Ink

@Composable
fun GameScreen(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        SoftPanel {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Игра",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Скоро будет доступна прокачка рангов и титулов, задания, профессии (Можно прокачивать цыгана или гея) и много крутого! Жду с вами",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Ink,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

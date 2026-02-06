package com.example.attack_of_the_mobile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

abstract class Minigame(val durationSeconds: Int) {
    abstract val title: String

    @Composable
    abstract fun Content(onComplete: (Boolean) -> Unit)
}

class MathMinigame : Minigame(durationSeconds = 7) {
    override val title: String = "Math Challenge"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val num1 by remember { mutableIntStateOf(Random.nextInt(0, 11)) }
        val num2 by remember { mutableIntStateOf(Random.nextInt(0, 11)) }
        var userAnswer by remember { mutableStateOf("") }
        val correctAnswer = num1 + num2

        var timeLeft by remember { mutableIntStateOf(durationSeconds) }

        LaunchedEffect(Unit) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            onComplete(false)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Time Left: $timeLeft", fontSize = 20.sp, color = if (timeLeft < 3) androidx.compose.ui.graphics.Color.Red else androidx.compose.ui.graphics.Color.Unspecified)
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "$num1 + $num2 = ?", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                value = userAnswer,
                onValueChange = {
                    userAnswer = it
                    if (it.toIntOrNull() == correctAnswer) {
                        onComplete(true)
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text("Type the answer") },
                singleLine = true
            )
        }
    }
}

class TapMinigame : Minigame(durationSeconds = 5) {
    override val title: String = "Speed Tap!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        var taps by remember { mutableIntStateOf(0) }
        val targetTaps = 10
        var timeLeft by remember { mutableIntStateOf(durationSeconds) }

        LaunchedEffect(Unit) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            onComplete(false)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = title, fontSize = 24.sp)
            Text(text = "Time Left: $timeLeft", fontSize = 20.sp)
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Taps: $taps / $targetTaps", fontSize = 32.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    taps++
                    if (taps >= targetTaps) {
                        onComplete(true)
                    }
                },
                modifier = Modifier.size(150.dp)
            ) {
                Text("TAP!", fontSize = 24.sp)
            }
        }
    }
}

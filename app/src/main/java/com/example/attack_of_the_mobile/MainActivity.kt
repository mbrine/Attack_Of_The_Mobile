package com.example.attack_of_the_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attack_of_the_mobile.ui.theme.Attack_Of_The_MobileTheme
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.ln

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Attack_Of_The_MobileTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    GameManager(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TimerBar(timeLeft: Double, maxTime: Double = 5.0) {
    val progress by animateFloatAsState(
        targetValue = (timeLeft / maxTime).toFloat().coerceIn(0f, 1f),
        label = "TimerProgress"
    )
    
    LinearProgressIndicator(
        progress = { progress },
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp),
        color = if (progress < 0.3f) Color.Red else MaterialTheme.colorScheme.primary,
        trackColor = Color.LightGray.copy(alpha = 0.3f)
    )
}

@Composable
fun GameManager(modifier: Modifier = Modifier) {
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableDoubleStateOf(5.0) }

    // Difficulty increases logarithmically with score
    val difficulty = ln((score + 1).toDouble() + 1.0) / ln(2.0)

    val minigames = listOf<(Double) -> Minigame>(
        { d -> MathMinigame(d) },
        { d -> MathMCQMinigame(d) },
        { d -> TapMinigame(d) },
        { d -> MovingTargetMinigame(d) },
        { d -> KnobMinigame(d) },
        { d -> ShakeMinigame(d) },
        { d -> VoiceMinigame(d) },
    )

    var currentMinigame by remember { mutableStateOf<Minigame?>(null) }
    var gameState by remember { mutableStateOf("START") } // START, PLAYING, RESULT, GAME_OVER

    LaunchedEffect(gameState) {
        if (gameState == "PLAYING") {
            val tickRate = 50L // ms
            while (timeLeft > 0 && gameState == "PLAYING") {
                delay(tickRate)
                timeLeft -= tickRate / 1000.0
                if (timeLeft <= 0) {
                    timeLeft = 0.0
                    gameState = "GAME_OVER"
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Show timer during gameplay and result screens
        if (gameState == "PLAYING" || gameState == "RESULT") {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                TimerBar(timeLeft = timeLeft)
            }
        }

        when (gameState) {
            "START" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Minigame Rush", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        score = 0
                        timeLeft = 5.0
                        currentMinigame = minigames.random()(difficulty)
                        gameState = "PLAYING"
                    }) {
                        Text("Start")
                    }
                }
            }
            "PLAYING" -> {
                key(currentMinigame) {
                    currentMinigame?.Content(onComplete = { success ->
                        if (success) {
                            // Weight based on difficulty to blend between instant refill and half-average gain
                            val minDifficulty = 1.0
                            val weight = (1.0 - exp(-(difficulty - minDifficulty))).coerceIn(0.0, 1.0)
                            
                            val targetTimeAtHighDifficulty = (5.0 + timeLeft) / 2.0
                            timeLeft = 5.0 * (1.0 - weight) + targetTimeAtHighDifficulty * weight
                            
                            score++
                            gameState = "RESULT"
                        } else {
                            gameState = "GAME_OVER"
                        }
                    })
                }
            }
            "RESULT" -> {
                LaunchedEffect(Unit) {
                    delay(1000)
                    currentMinigame = minigames.random()(difficulty)
                    gameState = "PLAYING"
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Success!", fontSize = 32.sp, color = Color.Green)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Score: $score", fontSize = 24.sp)
                    Text("Time Left: ${"%.1f".format(timeLeft)}s", fontSize = 18.sp)
                }
            }
            "GAME_OVER" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over", fontSize = 32.sp, color = Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Final Score: $score", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        score = 0
                        timeLeft = 5.0
                        currentMinigame = minigames.random()(difficulty)
                        gameState = "PLAYING"
                    }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

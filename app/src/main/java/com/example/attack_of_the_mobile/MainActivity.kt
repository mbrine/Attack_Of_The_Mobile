package com.example.attack_of_the_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
    val progress = (timeLeft / maxTime).toFloat().coerceIn(0f, 1f)
    
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
    var displayedTime by remember { mutableDoubleStateOf(0.0) }

    // Difficulty increases logarithmically with score
    val difficulty = ln((score + 1).toDouble() + 1.0) / ln(2.0)

    val minigameFactories = listOf<(Double) -> Minigame>(
        { d -> MathMinigame(d) },
        { d -> MathMCQMinigame(d) },
        { d -> TapMinigame(d) },
        { d -> MovingTargetMinigame(d) },
        { d -> KnobMinigame(d) },
        { d -> ShakeMinigame(d) },
        { d -> VoiceMinigame(d) },
        { d -> NoiseMinigame(d) },
    )

    var currentMinigame by remember { mutableStateOf<Minigame?>(null) }
    var nextMinigame by remember { mutableStateOf<Minigame?>(null) }
    var gameState by remember { mutableStateOf("START") } // START, TRANSITION, PLAYING, GAME_OVER

    val transitionDuration = 600L

    // Timer logic for the PLAYING state
    LaunchedEffect(gameState) {
        if (gameState == "PLAYING") {
            displayedTime = timeLeft
            val tickRate = 20L
            while (timeLeft > 0 && gameState == "PLAYING") {
                delay(tickRate)
                timeLeft -= tickRate / 1000.0
                displayedTime = timeLeft
                if (timeLeft <= 0) {
                    timeLeft = 0.0
                    displayedTime = 0.0
                    gameState = "GAME_OVER"
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // UI overlay (Timer, Score, and Skip Button)
        if (gameState == "PLAYING" || gameState == "TRANSITION") {
            Box(modifier = Modifier.fillMaxSize()) {
                // Timer and Score
                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                    TimerBar(timeLeft = displayedTime)
                    Text(
                        text = "Score: $score",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                // Skip Button
                if (gameState == "PLAYING") {
                    Button(
                        onClick = {
                            nextMinigame = minigameFactories.random()(difficulty)
                            gameState = "TRANSITION"
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .padding(top = 8.dp) // Offset to be below/beside timer bar
                    ) {
                        Text("Skip")
                    }
                }
            }
        }

        AnimatedContent(
            targetState = gameState,
            transitionSpec = {
                // Slide out left, Slide in right
                (slideInHorizontally(animationSpec = tween(transitionDuration.toInt())) { it } + fadeIn())
                    .togetherWith(slideOutHorizontally(animationSpec = tween(transitionDuration.toInt())) { -it } + fadeOut())
            },
            label = "StateTransition"
        ) { state ->
            when (state) {
                "START" -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minigame Rush", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            score = 0
                            timeLeft = 5.0
                            displayedTime = 0.0
                            nextMinigame = minigameFactories.random()(difficulty)
                            gameState = "TRANSITION"
                        }) {
                            Text("Start")
                        }
                    }
                }
                "TRANSITION" -> {
                    LaunchedEffect(Unit) {
                        val startTime = System.currentTimeMillis()
                        val startDisplayTime = displayedTime
                        while (System.currentTimeMillis() - startTime < transitionDuration) {
                            val elapsed = System.currentTimeMillis() - startTime
                            val fraction = elapsed.toDouble() / transitionDuration
                            displayedTime = startDisplayTime + (timeLeft - startDisplayTime) * fraction
                            delay(16)
                        }
                        displayedTime = timeLeft
                        currentMinigame = nextMinigame
                        gameState = "PLAYING"
                    }
                    
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = nextMinigame?.title ?: "Get Ready!",
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                "PLAYING" -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        key(currentMinigame) {
                            currentMinigame?.Content(onComplete = { success ->
                                if (success) {
                                    val minDifficulty = 1.0
                                    val weight = (1.0 - exp(-(difficulty - minDifficulty))).coerceIn(0.0, 1.0)
                                    val targetTimeAtHighDifficulty = (5.0 + timeLeft) / 2.0
                                    timeLeft = 5.0 * (1.0 - weight) + targetTimeAtHighDifficulty * weight
                                    
                                    score++
                                    nextMinigame = minigameFactories.random()(difficulty)
                                    gameState = "TRANSITION"
                                } else {
                                    gameState = "GAME_OVER"
                                }
                            })
                        }
                    }
                }
                "GAME_OVER" -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Game Over", fontSize = 48.sp, color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Final Score: $score", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            score = 0
                            timeLeft = 5.0
                            displayedTime = 0.0
                            nextMinigame = minigameFactories.random()(difficulty)
                            gameState = "TRANSITION"
                        }) {
                            Text("Try Again")
                        }
                    }
                }
            }
        }
    }
}

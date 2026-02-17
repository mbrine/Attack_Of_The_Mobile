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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import kotlin.math.exp
import kotlin.math.ln

data class MinigameEntry(
    val name: String,
    val factory: (Double) -> Minigame
)

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

// Sealed class to manage game state with associated data
sealed class GameState {
    data object Start : GameState()
    data class Transition(val nextGame: Minigame) : GameState()
    data class Playing(val game: Minigame) : GameState()
    data object GameOver : GameState()
}

@Composable
fun GameManager(modifier: Modifier = Modifier) {
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableDoubleStateOf(5.0) }
    var displayedTime by remember { mutableDoubleStateOf(0.0) }
    var gameState by remember { mutableStateOf<GameState>(GameState.Start) }
    var showSettings by remember { mutableStateOf(false) }

    val difficulty = ln((score + 1).toDouble() + 1.0) / ln(2.0)

    val allMinigames = remember {
        listOf(
            MinigameEntry("Math Challenge") { d -> MathMinigame(d) },
            MinigameEntry("Math MCQ") { d -> MathMCQMinigame(d) },
            MinigameEntry("Speed Tap") { d -> TapMinigame(d) },
            MinigameEntry("Catch the Button") { d -> MovingTargetMinigame(d) },
            MinigameEntry("Set the Dial") { d -> KnobMinigame(d) },
            MinigameEntry("Shake It") { d -> ShakeMinigame(d) },
            MinigameEntry("Say It") { d -> VoiceMinigame(d) },
            MinigameEntry("Make Some Noise") { d -> NoiseMinigame(d) },
        )
    }

    val enabledMinigames = remember { mutableStateMapOf<String, Boolean>().apply {
        allMinigames.forEach { put(it.name, true) }
    }}

    // Managed rotation list
    var rotationPool by remember { mutableStateOf(emptyList<MinigameEntry>()) }

    fun getNextMinigame(): Minigame {
        val activeEntries = allMinigames.filter { enabledMinigames[it.name] != false }
        
        // If pool is empty or only contains disabled games, refill and shuffle
        if (rotationPool.isEmpty() || rotationPool.all { enabledMinigames[it.name] == false }) {
            rotationPool = activeEntries.shuffled()
        }
        
        // Find first entry in pool that is still enabled
        val entry = rotationPool.first { enabledMinigames[it.name] != false }
        rotationPool = rotationPool.filter { it != entry }
        
        return entry.factory(difficulty)
    }

    val transitionDuration = 600L

    // Timer logic
    LaunchedEffect(gameState) {
        if (gameState is GameState.Playing) {
            displayedTime = timeLeft
            val tickRate = 20L
            while (timeLeft > 0 && gameState is GameState.Playing) {
                delay(tickRate)
                timeLeft -= tickRate / 1000.0
                displayedTime = timeLeft
                if (timeLeft <= 0) {
                    timeLeft = 0.0
                    displayedTime = 0.0
                    gameState = GameState.GameOver
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // UI Overlay: Timer, Score and Skip
        if (gameState is GameState.Playing || gameState is GameState.Transition) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.align(Alignment.TopCenter)) {
                    TimerBar(timeLeft = displayedTime)
                    Text(
                        text = "Score: $score",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.headlineSmall
                    )
                }

                if (gameState is GameState.Playing) {
                    Button(
                        onClick = {
                            val next = getNextMinigame()
                            gameState = GameState.Transition(next)
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .padding(top = 8.dp)
                    ) {
                        Text("Skip")
                    }
                }
            }
        }

        AnimatedContent(
            targetState = gameState,
            transitionSpec = {
                (slideInHorizontally(animationSpec = tween(transitionDuration.toInt())) { it } + fadeIn())
                    .togetherWith(slideOutHorizontally(animationSpec = tween(transitionDuration.toInt())) { -it } + fadeOut())
            },
            label = "GameStateTransition"
        ) { state ->
            when (state) {
                GameState.Start -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minigame Rush", fontSize = 32.sp)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = {
                            score = 0
                            timeLeft = 5.0
                            displayedTime = 0.0
                            rotationPool = emptyList() // Reset pool on new game
                            val firstGame = getNextMinigame()
                            gameState = GameState.Transition(firstGame)
                        }) {
                            Text("Start")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showSettings = true }) {
                            Text("Settings")
                        }
                    }
                }
                is GameState.Transition -> {
                    LaunchedEffect(state) {
                        val startTime = System.currentTimeMillis()
                        val startDisplayTime = displayedTime
                        while (System.currentTimeMillis() - startTime < transitionDuration) {
                            val elapsed = System.currentTimeMillis() - startTime
                            val fraction = (elapsed.toDouble() / transitionDuration).coerceIn(0.0, 1.0)
                            displayedTime = startDisplayTime + (timeLeft - startDisplayTime) * fraction
                            delay(16)
                        }
                        displayedTime = timeLeft
                        gameState = GameState.Playing(state.nextGame)
                    }
                    
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.nextGame.title,
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                is GameState.Playing -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        key(state.game) {
                            state.game.Content(onComplete = { success ->
                                if (gameState == state) {
                                    if (success) {
                                        val minDifficulty = 1.0
                                        val weight = (1.0 - exp(-(difficulty - minDifficulty))).coerceIn(0.0, 1.0)
                                        val targetTimeAtHighDifficulty = (5.0 + timeLeft) / 2.0
                                        timeLeft = 5.0 * (1.0 - weight) + targetTimeAtHighDifficulty * weight
                                        
                                        score++
                                        val next = getNextMinigame()
                                        gameState = GameState.Transition(next)
                                    } else {
                                        gameState = GameState.GameOver
                                    }
                                }
                            })
                        }
                    }
                }
                GameState.GameOver -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Game Over", fontSize = 48.sp, color = Color.Red)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Final Score: $score", fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(onClick = {
                            score = 0
                            timeLeft = 5.0
                            displayedTime = 0.0
                            rotationPool = emptyList() // Reset pool on retry
                            val next = getNextMinigame()
                            gameState = GameState.Transition(next)
                        }) {
                            Text("Try Again")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { showSettings = true }) {
                            Text("Settings")
                        }
                    }
                }
            }
        }

        if (showSettings) {
            val disabledCount = enabledMinigames.count { !it.value }
            val maxDisabled = 3

            AlertDialog(
                onDismissRequest = { showSettings = false },
                title = { Text("Settings") },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            "Disable up to $maxDisabled minigames ($disabledCount/$maxDisabled disabled)",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        allMinigames.forEach { entry ->
                            val enabled = enabledMinigames[entry.name] != false
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = enabled,
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            enabledMinigames[entry.name] = true
                                        } else if (disabledCount < maxDisabled) {
                                            enabledMinigames[entry.name] = false
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(entry.name, fontSize = 18.sp)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showSettings = false }) {
                        Text("Done")
                    }
                }
            )
        }
    }
}

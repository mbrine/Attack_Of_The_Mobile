package com.example.attack_of_the_mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.attack_of_the_mobile.ui.theme.Attack_Of_The_MobileTheme
import kotlinx.coroutines.delay

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
fun GameManager(modifier: Modifier = Modifier) {
    val minigames = listOf(
        { MathMinigame() },
        { TapMinigame() }
    )

    var currentMinigame by remember { mutableStateOf<Minigame?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var gameState by remember { mutableStateOf("START") } // START, PLAYING, RESULT, GAME_OVER

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (gameState) {
            "START" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Minigame Rush", fontSize = 32.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        currentMinigame = minigames.random()()
                        gameState = "PLAYING"
                    }) {
                        Text("Start")
                    }
                }
            }
            "PLAYING" -> {
                // key(currentMinigame) ensures the composable resets when the minigame object changes
                key(currentMinigame) {
                    currentMinigame?.Content(onComplete = { success ->
                        if (success) {
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
                    delay(1500)
                    currentMinigame = minigames.random()()
                    gameState = "PLAYING"
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Success!", fontSize = 32.sp, color = androidx.compose.ui.graphics.Color.Green)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Score: $score", fontSize = 24.sp)
                }
            }
            "GAME_OVER" -> {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Game Over", fontSize = 32.sp, color = androidx.compose.ui.graphics.Color.Red)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Final Score: $score", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = {
                        score = 0
                        currentMinigame = minigames.random()()
                        gameState = "PLAYING"
                    }) {
                        Text("Try Again")
                    }
                }
            }
        }
    }
}

package com.example.attack_of_the_mobile

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.*
import kotlin.random.Random

abstract class Minigame(val difficulty: Double) {
    abstract val title: String

    @Composable
    abstract fun Content(onComplete: (Boolean) -> Unit)
}

class MathMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Math Challenge"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val num1 by remember { mutableIntStateOf(Random.nextInt(0, 11 + (difficulty * 2).toInt())) }
        val num2 by remember { mutableIntStateOf(Random.nextInt(0, 11 + (difficulty * 2).toInt())) }
        var userAnswer by remember { mutableStateOf("") }
        val correctAnswer = num1 + num2

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 24.sp)
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
}

class MathMCQMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Math MCQ"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val range = 11 + (difficulty * 2).toInt()
        val num1 by remember { mutableIntStateOf(Random.nextInt(0, range)) }
        val num2 by remember { mutableIntStateOf(Random.nextInt(0, range)) }
        val correctAnswer = num1 + num2
        
        val options by remember {
            mutableStateOf(
                buildSet {
                    add(correctAnswer)
                    while (size < 3) {
                        val offset = Random.nextInt(-5, 6)
                        if (offset != 0) {
                            val decoy = correctAnswer + offset
                            if (decoy >= 0) add(decoy)
                        }
                    }
                }.toList().shuffled()
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "$num1 + $num2 = ?", fontSize = 48.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    options.forEach { option ->
                        Button(onClick = { onComplete(option == correctAnswer) }) {
                            Text(text = option.toString(), fontSize = 24.sp)
                        }
                    }
                }
            }
        }
    }
}

class TapMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Speed Tap!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        var taps by remember { mutableIntStateOf(0) }
        val targetTaps = 10 + (difficulty * 5).toInt()

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 24.sp)
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
}

class MovingTargetMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Catch the Button!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val buttonSize = 100.dp

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val scoop = this
            val density = LocalDensity.current
            val maxWidthPx = constraints.maxWidth.toFloat()
            val maxHeightPx = constraints.maxHeight.toFloat()
            val buttonSizePx = with(density) { buttonSize.toPx() }

            var posX by remember { mutableFloatStateOf(Random.nextFloat() * (maxWidthPx - buttonSizePx).coerceAtLeast(0f)) }
            var posY by remember { mutableFloatStateOf(Random.nextFloat() * (maxHeightPx - buttonSizePx).coerceAtLeast(0f)) }

            val speedBoost = 1f + difficulty.toFloat() * 0.05f
            var velX by remember {
                mutableFloatStateOf(with(density) { (Random.nextFloat() * 4 + 2).dp.toPx() } * (if (Random.nextBoolean()) 1f else -1f) * speedBoost)
            }
            var velY by remember {
                mutableFloatStateOf(with(density) { (Random.nextFloat() * 4 + 2).dp.toPx() } * (if (Random.nextBoolean()) 1f else -1f) * speedBoost)
            }

            LaunchedEffect(maxWidthPx, maxHeightPx) {
                while (true) {
                    posX += velX
                    posY += velY

                    if (posX <= 0f) {
                        posX = 0f
                        velX *= -1
                    } else if (posX >= maxWidthPx - buttonSizePx) {
                        posX = (maxWidthPx - buttonSizePx).coerceAtLeast(0f)
                        velX *= -1
                    }

                    if (posY <= 0f) {
                        posY = 0f
                        velY *= -1
                    } else if (posY >= maxHeightPx - buttonSizePx) {
                        posY = (maxHeightPx - buttonSizePx).coerceAtLeast(0f)
                        velY *= -1
                    }
                    delay(16)
                }
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                Text(text = title, fontSize = 24.sp)
            }

            Button(
                onClick = { onComplete(true) },
                modifier = Modifier
                    .offset { IntOffset(posX.toInt(), posY.toInt()) }
                    .size(buttonSize)
            ) {
                Text("TAP!")
            }
        }
    }
}

class KnobMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Set the Dial"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val targetAngle by remember { mutableFloatStateOf(Random.nextInt(0, 360).toFloat()) }
        val currentAngle = remember { mutableFloatStateOf(0f) }
        val tolerance = (15 / (1 + difficulty * 0.3)).toFloat().coerceAtLeast(2f)

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Target: ${targetAngle.toInt()}°", fontSize = 32.sp)
                Text(text = "Current: ${currentAngle.floatValue.toInt()}°", fontSize = 24.sp)
                Spacer(modifier = Modifier.height(32.dp))

                Box(
                    modifier = Modifier
                        .size(240.dp)
                        .pointerInput(Unit) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val position = change.position
                                val angle = atan2(position.y - center.y, position.x - center.x) * (180 / PI).toFloat()
                                currentAngle.floatValue = (angle + 360) % 360
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2
                        
                        // Outer ring
                        drawCircle(
                            color = Color.LightGray,
                            radius = radius,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                        )

                        // Target range
                        drawArc(
                            color = Color.Green.copy(alpha = 0.2f),
                            startAngle = targetAngle - tolerance,
                            sweepAngle = tolerance * 2,
                            useCenter = true
                        )

                        // Target line
                        rotate(targetAngle) {
                            drawLine(
                                color = Color.Red,
                                start = center,
                                end = Offset(center.x + radius, center.y),
                                strokeWidth = 3.dp.toPx()
                            )
                        }

                        // Knob body
                        rotate(currentAngle.floatValue) {
                            drawCircle(
                                color = Color.DarkGray,
                                radius = radius * 0.8f
                            )
                            // Indicator on knob
                            drawLine(
                                color = Color.White,
                                start = center,
                                end = Offset(center.x + radius * 0.8f, center.y),
                                strokeWidth = 6.dp.toPx()
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                Button(onClick = {
                    val diff = abs(currentAngle.floatValue - targetAngle) % 360
                    val normalizedDiff = if (diff > 180) 360 - diff else diff
                    onComplete(normalizedDiff <= tolerance)
                }) {
                    Text("SET", fontSize = 24.sp)
                }
            }
        }
    }
}

class ShakeMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Shake It!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val context = LocalContext.current
        var shakeCount by remember { mutableIntStateOf(0) }
        val targetShakes = 3 + (difficulty * 1).toInt()
        var shakePower by remember { mutableFloatStateOf(0f) }

        // Shake detection threshold
        val shakeThreshold = 15.0 + difficulty
        var lastShakeTime by remember { mutableLongStateOf(0L) }

        // Sensor setup
        DisposableEffect(context) {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val sensorListener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                        val x = event.values[0]
                        val y = event.values[1]
                        val z = event.values[2]

                        // Calculate acceleration magnitude
                        val acceleration = sqrt((x * x + y * y + z * z).toDouble())

                        // Update shake power for visual feedback
                        shakePower = (acceleration - 9.8).toFloat().coerceIn(0f, 20f) / 20f

                        val currentTime = System.currentTimeMillis()

                        // Detect shake
                        if (acceleration > shakeThreshold &&
                            currentTime - lastShakeTime > 200) {
                            shakeCount++

                            // Check for win condition
                            if (shakeCount >= targetShakes) {
                                onComplete(true)
                            }
                        }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
            }

            sensorManager.registerListener(
                sensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_GAME
            )

            onDispose {
                sensorManager.unregisterListener(sensorListener)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(32.dp))

                // Shake power meter
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Power bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(24.dp)
                            .background(
                                color = Color.LightGray,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(shakePower)
                                .fillMaxHeight()
                                .background(
                                    color = when {
                                        shakePower > 0.7f -> Color.Green
                                        shakePower > 0.3f -> Color.Yellow
                                        else -> Color.Red
                                    },
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Progress bar for shake count
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Progress: $shakeCount / $targetShakes",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(24.dp)
                            .background(
                                color = Color.DarkGray,
                                shape = MaterialTheme.shapes.small
                            )
                    ) {
                        val progress = (shakeCount.toFloat() / targetShakes).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(
                                    color = Color.White,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(128.dp))
            }
        }
    }
}

class VoiceMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Say it!"
    
    private val phoneticAlphabet = listOf(
        "Alpha", "Bravo", "Charlie", "Delta", "Echo", "Foxtrot", "Golf", "Hotel", "India",
        "Juliet", "Kilo", "Lima", "Mike", "November", "Oscar", "Papa", "Quebec", "Romeo",
        "Sierra", "Tango", "Uniform", "Victor", "Whiskey", "X-ray", "Yankee", "Zulu"
    )

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val context = LocalContext.current
        val targetWord by remember { mutableStateOf(phoneticAlphabet.random()) }
        var recognizedText by remember { mutableStateOf("") }
        var isListening by remember { mutableStateOf(false) }
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            hasPermission = granted
        }

        DisposableEffect(hasPermission) {
            var speechRecognizer: SpeechRecognizer? = null
            if (hasPermission) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                }

                speechRecognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) { isListening = true }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListening = false }
                    override fun onError(error: Int) { isListening = false }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.forEach { match ->
                            if (match.equals(targetWord, ignoreCase = true)) {
                                onComplete(true)
                                return
                            }
                        }
                        recognizedText = matches?.firstOrNull() ?: "Try again"
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                speechRecognizer.startListening(intent)
            }

            onDispose {
                speechRecognizer?.destroy()
            }
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = title, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "Say:", fontSize = 18.sp)
                Text(text = targetWord, fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(32.dp))
                
                if (!hasPermission) {
                    Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Grant Microphone Permission")
                    }
                } else {
                    Text(
                        text = if (isListening) "Listening..." else "Recognition: $recognizedText",
                        fontSize = 18.sp,
                        color = if (isListening) Color.Blue else Color.Gray
                    )
                }
            }
        }
    }
}

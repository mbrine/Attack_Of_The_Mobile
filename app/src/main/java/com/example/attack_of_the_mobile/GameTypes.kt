package com.example.attack_of_the_mobile

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

/*
Title: Solve the Math!

Written by: Matthew Chan

Summary: Type the correct math answer. The game automatically completes if the correct answer is present.
Difficulty effects: Range of numbers increases.
*/
class MathMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Solve the Math!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        var num1 by remember { mutableIntStateOf(Random.nextInt(0, 11 + (difficulty * 2).toInt())) }
        var num2 by remember { mutableIntStateOf(Random.nextInt(0, 11 + (difficulty * 2).toInt())) }
        var userAnswer by remember { mutableStateOf("")}
        val isAddition by remember { mutableStateOf(Random.nextBoolean()) }
        if(!isAddition)
        {
            if(num2>num1)
            {
                val tmp = num1
                num1 = num2
                num2 = tmp
            }
        }
        val correctAnswer = if(isAddition) num1 + num2 else num1 - num2
        val operator = if(isAddition) "+" else "-"

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "$num1 $operator $num2 = ?", fontSize = 48.sp)
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

/*
Title: MCQ Math!

Written by: Matthew Chan

Summary: Tap the correct math answer. Wrong answer will end the game.
Difficulty effects: Range of numbers increases, and 4-option questions are more likely to appear.
*/
class MathMCQMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "MCQ Math!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val range = 11 + (difficulty * 2).toInt()
        val num1 by remember { mutableIntStateOf(Random.nextInt(0, range)) }
        val num2 by remember { mutableIntStateOf(Random.nextInt(0, range)) }
        val correctAnswer = num1 + num2

        val numAnswers = if (Random.nextFloat() * difficulty>difficulty*0.65) 4 else 3


        val options by remember {
            mutableStateOf(
                buildSet {
                    add(correctAnswer)
                    while (size < numAnswers) {
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

/*
Title: Speed Tap!

Written by: Matthew Chan

Summary: Tap the screen for the required number of times.
Difficulty effects: Required taps increases.
*/

class TapMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Speed Tap!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        var taps by remember { mutableIntStateOf(0) }
        val targetTaps = 10 + (difficulty * 5).toInt()

        Box(modifier = Modifier.fillMaxSize().clickable(){
            taps++
            if (taps >= targetTaps) {
                onComplete(true)
            }
        }) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 24.sp)
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = "Taps: $taps / $targetTaps", fontSize = 32.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("TAP ANYWHERE!", fontSize = 24.sp)
            }
        }
    }
}

/*
Title: Catch the Button!

Written by: Matthew Chan

Summary: Tap the moving button.
Difficulty effects: Button speed increases.
*/
class MovingTargetMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Catch the Button!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val buttonSize = 150.dp

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

/*
Title: Set the Dial!

Written by: Matthew Chan

Summary: Rotate the dial to the correct angle range and click the Set button.
Difficulty effects: Angle tolerance decreases, down to 2 degrees.
*/
class KnobMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Set the Dial!"

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
                Text(text = "Target: ${(targetAngle - tolerance).toInt()} - ${(targetAngle + tolerance).toInt()}°", fontSize = 32.sp)
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

/*
Title: Shake It!

Written by: Ryan Chan

Summary: Shake the phone.
Difficulty effects: Number of required shakes increases, shake strength threshold increases.
*/
class ShakeMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Shake It!"

    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val context = LocalContext.current
        var shakeCount by remember { mutableIntStateOf(0) }
        val targetShakes = 35 + (difficulty * 5).toInt()
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

/*
Title: Make Some Noise!

Written by: Bryan Chua

Summary: Make noise within a volume range, a certain number of times.
Difficulty effects: Volume must be maintained for longer.
*/
class NoiseMinigame(difficulty: Double) : Minigame(difficulty) {
    override val title: String = "Make Some Noise!"

    @SuppressLint("MissingPermission")
    @Composable
    override fun Content(onComplete: (Boolean) -> Unit) {
        val context = LocalContext.current
        var currentDb by remember { mutableFloatStateOf(0f) }
        var thresholdsHit by remember { mutableIntStateOf(0) }
        val totalThresholds = 3
        val rangeSize = 20
        val ranges by remember {
            mutableStateOf(List(totalThresholds) {
                val low = Random.nextInt(30, 70 - rangeSize)
                low.toFloat() to (low + rangeSize).toFloat()
            })
        }

        val cooldown = 300L
        val holdRequired = ((2.0 + difficulty - (totalThresholds - 1) * cooldown / 1000.0) / totalThresholds)
            .coerceIn(0.1, 0.7).toFloat()
        var holdProgress by remember { mutableFloatStateOf(0f) }
        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted -> hasPermission = granted }

        DisposableEffect(hasPermission) {
            var isRecording = true
            var audioRecord: AudioRecord? = null

            if (hasPermission) {
                val sampleRate = 44100
                val bufferSize = AudioRecord.getMinBufferSize(
                    sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
                )
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC, sampleRate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize
                )
                audioRecord.startRecording()

                Thread {
                    val buffer = ShortArray(bufferSize)
                    while (isRecording) {
                        val read = audioRecord.read(buffer, 0, bufferSize)
                        if (read > 0) {
                            var sum = 0.0
                            for (i in 0 until read) {
                                sum += buffer[i].toDouble() * buffer[i].toDouble()
                            }
                            val rms = sqrt(sum / read)
                            currentDb = (20 * log10(rms + 1)).toFloat()
                        }
                    }
                }.start()
            }

            onDispose {
                isRecording = false
                audioRecord?.stop()
                audioRecord?.release()
            }
        }

        // Tick-based hold detection
        LaunchedEffect(hasPermission) {
            if (!hasPermission) return@LaunchedEffect
            val tickRate = 50L
            while (thresholdsHit < totalThresholds) {
                delay(tickRate)
                val (low, high) = ranges[thresholdsHit]
                if (currentDb in low..high) {
                    holdProgress += tickRate / 1000f
                    if (holdProgress >= holdRequired) {
                        thresholdsHit++
                        holdProgress = 0f
                        if (thresholdsHit >= totalThresholds) {
                            onComplete(true)
                        } else {
                            delay(cooldown)
                        }
                    }
                } else {
                    // Keep current hold progress — don't reset
                }
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

                if (!hasPermission) {
                    Button(onClick = { launcher.launch(Manifest.permission.RECORD_AUDIO) }) {
                        Text("Grant Microphone Permission")
                    }
                } else {
                    val (targetLow, targetHigh) = if (thresholdsHit < totalThresholds) ranges[thresholdsHit] else (0f to 0f)
                    Text(text = "Hit ${targetLow.toInt()} - ${targetHigh.toInt()} dB!", fontSize = 32.sp)
                    Text(text = "Hold for ${"%.1f".format(holdRequired)}s", fontSize = 18.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    // dB meter
                    val maxDb = 90f
                    val dbNormalized = (currentDb / maxDb).coerceIn(0f, 1f)
                    val targetLowNorm = (targetLow / maxDb).coerceIn(0f, 1f)
                    val targetHighNorm = (targetHigh / maxDb).coerceIn(0f, 1f)
                    val inRange = currentDb in targetLow..targetHigh

                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(200.dp)
                            .background(Color.DarkGray, MaterialTheme.shapes.small)
                    ) {
                        // Target range zone
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((targetHighNorm - targetLowNorm).coerceAtLeast(0f))
                                .align(Alignment.BottomCenter)
                                .offset(y = -(200.dp * targetLowNorm))
                                .background(Color.Green.copy(alpha = 0.3f))
                        )
                        // Current level fill
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(dbNormalized)
                                .align(Alignment.BottomCenter)
                                .background(
                                    if (inRange) Color.Green else Color.Red,
                                    MaterialTheme.shapes.small
                                )
                        )
                        // Target low line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter)
                                .offset(y = -(200.dp * targetLowNorm))
                                .background(Color.White)
                        )
                        // Target high line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .align(Alignment.BottomCenter)
                                .offset(y = -(200.dp * targetHighNorm))
                                .background(Color.White)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${currentDb.toInt()} dB", fontSize = 24.sp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Hold timer bar
                    val holdNormalized = (holdProgress / holdRequired).coerceIn(0f, 1f)
                    Text(text = "Hold: ${"%.1f".format(holdProgress)}s / ${"%.1f".format(holdRequired)}s", fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(16.dp)
                            .background(Color.DarkGray, MaterialTheme.shapes.small)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(holdNormalized)
                                .fillMaxHeight()
                                .background(
                                    if (inRange) Color.Cyan else Color.Gray,
                                    MaterialTheme.shapes.small
                                )
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))

                    // Progress
                    Text(
                        text = "Progress: $thresholdsHit / $totalThresholds",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .height(24.dp)
                            .background(Color.DarkGray, MaterialTheme.shapes.small)
                    ) {
                        val progress = (thresholdsHit.toFloat() / totalThresholds).coerceIn(0f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(progress)
                                .fillMaxHeight()
                                .background(Color.White, MaterialTheme.shapes.small)
                        )
                    }
                }
            }
        }
    }
}

/*
Title: Say It!

Written by: Matthew Chan

Summary: Say the word on screen.
Difficulty effects: None.
*/
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

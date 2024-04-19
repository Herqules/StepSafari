package com.example.myapplication

import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = mutableStateOf(0f)
    private var stepsOffset = 0F // Used to offset steps to 0 at the beginning of challenge
    private var stepsrawdata = 0F // Raw data read from step counter sensor

    private val chromeColor = Color(android.graphics.Color.parseColor("#f9edd2"))
    private val safariColor = Color(android.graphics.Color.parseColor("#ecb604"))
    private val safariAccentColor = Color(android.graphics.Color.parseColor("#9c260a"))
    private val safariDarkColor = Color(android.graphics.Color.parseColor("#91825e"))
    private val deadCard = Color(android.graphics.Color.parseColor("#ada28b"))

    private var currentScreen = mutableStateOf("Activity") // Current Screen
    private var activityState = mutableStateOf("Select") // Select VS Running

    private var selectedChallenge = 0

    // Challenges Values: Name [Picture in future], steps, speed, time, Unlocked?
    private val challenges = listOf(
        listOf("Tiger challenge", 4, 8, 12, false),
        listOf("Bear challenge", 12, 8, 4, true),
        listOf("Puma challenge", 12, 7, 4, false),
        listOf("Parrot challenge", 4, 8, 12, false),
        listOf("Dog challenge", 12, 8, 4, true),
        listOf("Monkey challenge", 12, 7, 4, true),
        listOf("Owl challenge", 4, 8, 12, false),
        listOf("Beaver challenge", 12, 8, 4, true),
        listOf("Rabbit challenge", 12, 7, 4, true)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
            // Handle case where the step counter sensor is not available
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainLayout()
                }
            }
        }
    }

    @Composable
    fun MainLayout() {
        val backgroundImage: Painter = painterResource(id = R.drawable.bg)

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                painter = backgroundImage,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            Column(modifier = Modifier.fillMaxSize()) {
                TitleBar()
                when {
                    currentScreen.value == "Activity" && activityState.value == "Select" -> SelectScreen()
                    currentScreen.value == "Activity" -> ActivityScreen()
                    currentScreen.value == "Achievements" -> AchievementsScreen()
                    currentScreen.value == "Possibilities" -> PossibilitiesScreen()
                }
                Spacer(modifier = Modifier.weight(1f))
                BottomNavigationBar()
            }
        }
    }

    @Composable
    fun TitleBar() {
        Box(modifier = Modifier
                .fillMaxWidth()
                //.shadow(elevation = 10.dp)
                .background(
                    color = chromeColor,
                    shape = RoundedCornerShape(
                        topStart = CornerSize(0.dp),
                        topEnd = CornerSize(0.dp),
                        bottomStart = CornerSize(32.dp),
                        bottomEnd = CornerSize(32.dp)
                    )
                )
                .padding(24.dp)
        ) {
            Text(
                text = "StepSafari",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 48.sp),
                color = safariColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }

    @Composable
    fun BottomNavigationBar() {
        Box(modifier = Modifier
            .fillMaxWidth()
            .background(
                color = chromeColor,
                shape = RoundedCornerShape(
                    topStart = CornerSize(32.dp),
                    topEnd = CornerSize(32.dp),
                    bottomStart = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp)
                )
            )
            .padding(24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Button for "Activity"
                IconButton(onClick = { currentScreen.value = "Activity" }) {
                    Icon(
                        painter = painterResource(id = R.drawable.stopwatch),
                        contentDescription = "Activity",
                        modifier = Modifier.size(60.dp),
                        tint = if (currentScreen.value == "Activity") safariAccentColor else safariColor
                    )
                }
                // Button for "Achievements"
                IconButton(onClick = { currentScreen.value = "Achievements" }) {
                    Icon(
                        painter = painterResource(id = R.drawable.trophy),
                        contentDescription = "Achievements",
                        modifier = Modifier.size(60.dp),
                        tint = if (currentScreen.value == "Achievements") safariAccentColor else safariColor
                    )
                }
                // Button for "Possibilities"
                IconButton(onClick = { currentScreen.value = "Possibilities" }) {
                    Icon(
                        painter = painterResource(id = R.drawable.binoc),
                        contentDescription = "Possibilities",
                        modifier = Modifier.size(60.dp),
                        tint = if (currentScreen.value == "Possibilities") safariAccentColor else safariColor
                    )
                }
            }
        }
    }

    @Composable
    fun SelectScreen() {
        var selectedIndex by remember { mutableStateOf(0) } // Default to the first index

        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(onClick = {
                    selectedIndex = if (selectedIndex > 0) selectedIndex - 1 else challenges.size - 1
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.triangleb),
                        contentDescription = "Previous Challenge",
                        modifier = Modifier.size(60.dp),
                        tint = chromeColor
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Challenge(challengeDetails = challenges[selectedIndex])
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    selectedIndex = (selectedIndex + 1) % challenges.size
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.triangle),
                        contentDescription = "Next Challenge",
                        modifier = Modifier.size(60.dp),
                        tint = chromeColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    activityState.value = "Running"
                    selectedChallenge = selectedIndex
                    totalSteps.value = 0F
                    stepsOffset = stepsrawdata
                },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(230.dp),
                colors = ButtonDefaults.buttonColors(containerColor = safariColor) // Correct parameter for background color
            ) {
                Text("Select")
            }
        }
    }


    @Composable
    fun ActivityScreen() {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Steps taken: ${totalSteps.value.toInt()}", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { totalSteps.value += 1 }) {
                Text("Increment Steps")
            }
            Challenge(challengeDetails = challenges[selectedChallenge])
            Button(onClick = { activityState.value = "Select" }) {
                Text("End Attempt")
            }
        }
    }

    @Composable
    fun AchievementsScreen() {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Unlocked Challenges", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(challenges.size) { index ->
                    if (challenges[index][4] as Boolean) {
                        // Not Unlocked
                    } else {
                        Challenge(challengeDetails = challenges[index])
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    @Composable
    fun PossibilitiesScreen() {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "It's possible!", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(challenges.size) { index ->
                    Challenge(challengeDetails = challenges[index])
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    @Composable
    fun Challenge(challengeDetails: List<Any>) {
        val challengeName = challengeDetails[0] as String
        val maxProgress = 12  // Assuming 12 is the maximum value for progress bars for visual scaling

        Box(
            modifier = Modifier
                .background(if (challengeDetails[4] as Boolean && currentScreen.value == "Possibilities") deadCard else chromeColor, shape = RoundedCornerShape(16.dp))
                .width(230.dp)
                .height(100.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column {
                Text(text = challengeName, style = MaterialTheme.typography.bodyMedium, color = safariColor)
                Spacer(modifier = Modifier.height(4.dp))
                challengeDetails.drop(1).dropLast(1).forEach {
                    val progress = it as Int
                    ProgressBar(progress, maxProgress, safariColor)
                }
            }
        }
    }

    @Composable
    fun ProgressBar(progress: Int, maxProgress: Int, color: Color) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(safariDarkColor)
        ) {
            Box(
                modifier = Modifier
                    .background(color)
                    .height(8.dp)
                    .width((230.dp * progress / maxProgress))  // Calculating width based on progress
            )
        }
        Spacer(modifier = Modifier.height(4.dp))  // Spacing between each progress bar
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Implement logic if needed to handle sensor accuracy changes
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            stepsrawdata = event.values[0]
            totalSteps.value += stepsrawdata - totalSteps.value - stepsOffset
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
    }
}

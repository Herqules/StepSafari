package com.example.myapplication

import android.os.Bundle
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var totalSteps = mutableStateOf(0f)
    private var stepsOffset = 0F
    private var stepsrawdata = 0F

    private val chromeColor = Color(android.graphics.Color.parseColor("#f9edd2"))
    private val safariColor = Color(android.graphics.Color.parseColor("#ecb604"))
    private val safariAccentColor = Color(android.graphics.Color.parseColor("#9c260a"))
    private val safariDarkColor = Color(android.graphics.Color.parseColor("#91825e"))
    private val deadCard = Color(android.graphics.Color.parseColor("#ada28b"))

    private var currentScreen = mutableStateOf("Activity") // Current Screen
    private var activityState = mutableStateOf("Select") // Select VS Running
    private var popup = mutableStateOf("None")

    private var selectedChallenge = 0
    private var timetaken = mutableStateOf(0f)

    private var ticks = 0

    private var challenges = listOf(
        listOf(R.drawable.tiger, 6, 8, 3, true),
        listOf(R.drawable.bear, 8, 3, 8, true),
        listOf(R.drawable.puma, 5, 7, 4, true),
        listOf(R.drawable.parrot, 4, 5, 6, true),
        listOf(R.drawable.dog, 5, 5, 5, true),
        listOf(R.drawable.monkey, 6, 4, 4, true),
        listOf(R.drawable.owl, 3, 6, 6, true),
        listOf(R.drawable.beaver, 8, 4, 8, true),
        listOf(R.drawable.rabbit, 6, 8, 2, true)
    )

    private var challengelocks = mutableListOf(
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true,
        true
    )

    fun updateChallengeLocksEntry(entrynum: Int, value: Boolean) {
        challengelocks[entrynum] = value
        saveChallengeLocks()
        updateChallengeLocks()
    }

    fun saveChallengeLocks() {
        val sharedPreferences = getSharedPreferences("StepSafariPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val lockString = challengelocks.joinToString(separator = ",") { it.toString() }

        editor.putString("ChallengeLocks", lockString)
        editor.apply()
    }

    fun loadChallengeLocks() {
        Log.d("MainActivity", "Starting to load challenge locks")
        val sharedPreferences = getSharedPreferences("StepSafariPrefs", MODE_PRIVATE)

        val lockString = sharedPreferences.getString("ChallengeLocks", "")
        Log.d("MainActivity", "Retrieved lock string from SharedPreferences: '$lockString'")

        if (!lockString.isNullOrEmpty()) {
            challengelocks = lockString.split(",").map {
                try {
                    it.toBoolean()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error parsing boolean value from string: $it", e)
                    true
                }
            }.toMutableList()
            Log.d("MainActivity", "Converted lockString to challengelocks: $challengelocks")

            updateChallengeLocks()
        } else {
            Log.d("MainActivity", "No lock string found, using default values")
        }
    }

    fun updateChallengeLocks() {
        Log.d("MainActivity", "Starting to update challenge locks")
        if (challenges.size == challengelocks.size) {
            challenges = challenges.mapIndexed { index, challenge ->
                challenge.dropLast(1) + challengelocks[index]
            }
            Log.d("MainActivity", "Updated challenges list with new lock statuses: $challenges")
        } else {
            Log.e("MainActivity", "The size of challenges does not match the size of challengelocks")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        loadChallengeLocks()

        updateChallengeLocks()
        //saveChallengeLocks()


        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        if (stepSensor != null) {
            sensorManager.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        } else {
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
    fun ComposeMapView() {
        val mapView = rememberMapViewWithLifecycle()

        AndroidView({ mapView }) { mapView ->
            mapView.getMapAsync { googleMap ->
                val sydney = LatLng(-34.0, 151.0)
                googleMap.addMarker(
                    MarkerOptions()
                        .position(sydney)
                        .title("Marker in Sydney")
                )
                googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney))
            }
        }
    }

    @Composable
    fun rememberMapViewWithLifecycle(): MapView {
        val context = LocalContext.current
        val mapView = remember {
            MapView(context).apply {
                onCreate(null)
                onResume()
                getMapAsync(OnMapReadyCallback { googleMap ->
                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL)
                })
            }
        }

        val lifecycle = LocalLifecycleOwner.current.lifecycle
        DisposableEffect(lifecycle) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                    Lifecycle.Event.ON_START -> mapView.onStart()
                    Lifecycle.Event.ON_RESUME -> mapView.onResume()
                    Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                    Lifecycle.Event.ON_STOP -> mapView.onStop()
                    Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                    else -> throw IllegalStateException()
                }
            }
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }

        return mapView
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
                IconButton(onClick = {
                    currentScreen.value = "Activity"
                    popup.value = "None"
                }) {
                    Icon(
                        painter = painterResource(id = R.drawable.stopwatch),
                        contentDescription = "Activity",
                        modifier = Modifier.size(60.dp),
                        tint = if (currentScreen.value == "Activity") safariAccentColor else safariColor
                    )
                }
                IconButton(onClick = {
                    currentScreen.value = "Achievements"}) {
                    Icon(
                        painter = painterResource(id = R.drawable.trophy),
                        contentDescription = "Achievements",
                        modifier = Modifier.size(60.dp),
                        tint = if (currentScreen.value == "Achievements") safariAccentColor else safariColor
                    )
                }
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
        var selectedIndex by remember { mutableStateOf(0) }



        Column(modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
        ) {

            if(popup.value == "Fail"){
                Text(text = "Too slow! Try again!", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if(popup.value == "Win"){
                Text(text = "Congrats!", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(4.dp))
            }

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
                colors = ButtonDefaults.buttonColors(containerColor = safariColor)
            ) {
                Text("Select")
            }
        }
    }


    @Composable
    fun ActivityScreen() {
        val ticker = remember { mutableStateOf(0) }
        ticks = 0
        val speed = remember { mutableStateOf(0) }
        val avgspeed = remember { mutableStateOf((challenges[selectedChallenge][2] as Int).toFloat()) }
        val avgavgspeed = remember { mutableStateOf((challenges[selectedChallenge][2] as Int).toFloat()) }
        var laststeps = remember { mutableStateOf(totalSteps.value.toInt()) }

        val amountUnder = remember { mutableStateOf(0f) }

        LaunchedEffect(Unit) {
            while (true) {
                delay(333)
                ticker.value = (ticker.value + 1) % Int.MAX_VALUE
                timetaken.value += 0.05f
                ticks += 1

                speed.value = ((totalSteps.value.toInt() - laststeps.value) / ticks) * 10
                laststeps.value = totalSteps.value.toInt()

                avgspeed.value = (speed.value + (avgspeed.value * 4)) / 5
                avgavgspeed.value = (avgspeed.value + (avgavgspeed.value * 4)) / 5

                amountUnder.value += (avgavgspeed.value - (challenges[selectedChallenge][2] as Int).toFloat())

                if(amountUnder.value < -10f){
                    popup.value = "Fail"
                    activityState.value = "Select"

                }

                if((timetaken.value > challenges[selectedChallenge][3] as Int) && (totalSteps.value.toFloat() / 10) > challenges[selectedChallenge][1] as Int){
                    updateChallengeLocksEntry(selectedChallenge, false)
                    popup.value = "Win"
                    activityState.value = "Select"
                }

            }
        }

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Challenge(challengeDetails = challenges[selectedChallenge], (totalSteps.value.toFloat() / 10), avgavgspeed.value, timetaken.value)

            /**
            Spacer(modifier = Modifier.height(120.dp))
            Text(text = "Steps taken: ${totalSteps.value.toInt()}", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = { totalSteps.value += 1 }) {
                Text("Increment Steps")
            }


            Button(onClick = {
                updateChallengeLocksEntry(selectedChallenge, false)
                saveChallengeLocks()
            }) {
                Text("Win challenge")
            }

            Button(onClick = {
                activityState.value = "Select"
            }) {
                Text("End Attempt")
            }
            */
            ComposeMapView()
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
    fun Challenge(
        challengeDetails: List<Any>,
        mark1: Float? = null,
        mark2: Float? = null,
        mark3: Float? = null
    ) {
        val maxProgress = 12

        Box(
            modifier = Modifier
                .background(
                    if (challengeDetails[4] as Boolean && currentScreen.value == "Possibilities")
                        deadCard
                    else
                        chromeColor,
                    shape = RoundedCornerShape(16.dp)
                )
                .width(230.dp)
                .height(160.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Column {
                Image(
                    painter = painterResource(id = challengeDetails[0] as Int),
                    contentDescription = "Challenge Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.height(4.dp))
                challengeDetails.drop(1).dropLast(1).forEachIndexed { index, it ->
                    val progress = it as Int
                    val mark = when (index) {
                        0 -> mark1
                        1 -> mark2
                        2 -> mark3
                        else -> null
                    }
                    val iconId = when (index) {
                        0 -> R.drawable.shoe
                        1 -> R.drawable.speed
                        2 -> R.drawable.stopwatch
                        else -> 0
                    }
                    ProgressBar(progress, maxProgress, safariColor, mark, iconId)
                }
            }
        }
    }

    @Composable
    fun ProgressBar(
        progress: Int,
        maxProgress: Int,
        color: Color,
        mark: Float? = null,
        iconId: Int
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (iconId != 0) {
                Icon(
                    painter = painterResource(id = iconId),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
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
                        .width((230.dp * progress / maxProgress))
                )
                mark?.let {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .offset(x = (230.dp * it / maxProgress - 4.dp), y = 0.dp)
                            .size(8.dp)
                            .background(safariAccentColor)
                    )
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
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

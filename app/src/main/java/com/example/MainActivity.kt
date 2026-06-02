package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.data.*
import com.example.ui.DashboardViewModel
import com.example.ui.FinanceTabScreen
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.LayerCard
import com.example.ui.theme.MutedText
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.InstaPurple
import com.example.ui.theme.InstaRed
import com.example.ui.theme.InstaOrange
import com.example.ui.theme.BrandAccent
import java.util.Calendar
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: DashboardRepository
    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "umar_tracker_db_v5"
        ).fallbackToDestructiveMigration().build()

        repository = DashboardRepository(database.dashboardDao())

        // ViewModel Factory
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(repository) as T
            }
        })[DashboardViewModel::class.java]

        // Schedule daily notification alarm at 11:00 AM
        try {
            NotificationReceiver.scheduleDailyNotification(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Request notification permission if running on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Track app open streak
        val appOpenStreak = updateAppOpenStreak(this)

        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }
                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else {
                    MainAppContainer(viewModel = viewModel, appOpenStreak = appOpenStreak)
                }
            }
        }
    }
}

fun updateAppOpenStreak(context: android.content.Context): Int {
    val prefs = context.getSharedPreferences("app_streak_prefs", android.content.Context.MODE_PRIVATE)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = sdf.format(Date())
    val lastOpenDate = prefs.getString("last_open_date", "") ?: ""
    val currentStreak = prefs.getInt("open_streak", 0)
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)
    return when {
        lastOpenDate == todayStr -> currentStreak
        lastOpenDate == yesterdayStr -> {
            val newStreak = currentStreak + 1
            prefs.edit().putString("last_open_date", todayStr).putInt("open_streak", newStreak).apply()
            newStreak
        }
        else -> {
            prefs.edit().putString("last_open_date", todayStr).putInt("open_streak", 1).apply()
            1
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var alpha by remember { mutableStateOf(0f) }
    val animAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = androidx.compose.animation.core.tween(700, easing = EaseInOut),
        label = "splash_alpha"
    )
    var scale by remember { mutableStateOf(0.85f) }
    val animScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = androidx.compose.animation.core.tween(700, easing = EaseInOut),
        label = "splash_scale"
    )

    LaunchedEffect(Unit) {
        alpha = 1f
        scale = 1f
        kotlinx.coroutines.delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(animAlpha)
                .scale(animScale)
        ) {
            Text(
                text = "UmarOS",
                style = TextStyle(
                    brush = InstaGradient,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personal Self-Improvement Hub",
                color = Color(0xFF666666),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = InstaOrange,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

// Global gradient brush
val InstaGradient = Brush.linearGradient(
    colors = listOf(InstaPurple, InstaRed, InstaOrange)
)

@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default
) {
    Text(
        text = text,
        modifier = modifier.drawBehind {  },
        style = style.copy(brush = InstaGradient)
    )
}

@Composable
fun MainAppContainer(viewModel: DashboardViewModel, appOpenStreak: Int = 1) {
    val tabIds = listOf("today", "goals", "learning", "sleep", "stats", "finance", "week")
    val pagerState = rememberPagerState(initialPage = 0) { tabIds.size }
    var selectedTab by rememberSaveable { mutableStateOf("today") }

    // Keep pager and bottom nav in sync
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = tabIds[pagerState.currentPage]
    }
    LaunchedEffect(selectedTab) {
        val idx = tabIds.indexOf(selectedTab)
        if (idx >= 0 && pagerState.currentPage != idx) {
            pagerState.animateScrollToPage(idx)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg),
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasBg)
                .padding(innerPadding)
        ) {
            AppHeader(viewModel = viewModel, appOpenStreak = appOpenStreak)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = true
            ) { page ->
                when (tabIds[page]) {
                    "today" -> TodayTabScreen(viewModel = viewModel)
                    "goals" -> GoalsTabScreen(viewModel = viewModel)
                    "learning" -> LearningTabScreen(viewModel = viewModel)
                    "sleep" -> SleepTabScreen(viewModel = viewModel)
                    "stats" -> StatsTabScreen(viewModel = viewModel)
                    "finance" -> FinanceTabScreen(viewModel = viewModel)
                    "week" -> WeekTabScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppHeader(viewModel: DashboardViewModel, appOpenStreak: Int = 1) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "UMAR KHAN",
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
                    brush = InstaGradient
                )
            )
            Text(
                text = "Personal Self-Improvement Hub",
                style = TextStyle(
                    color = MutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        // App open streak pill
        Row(
            modifier = Modifier
                .background(LayerCard, shape = RoundedCornerShape(100))
                .border(1.dp, BorderHighlight, shape = RoundedCornerShape(100))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "🔥", fontSize = 14.sp)
            Text(
                text = "$appOpenStreak DAY${if (appOpenStreak != 1) "S" else ""}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color(0xFF111111),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabItems = listOf(
                TabItem("today", "Today", Icons.Default.Home),
                TabItem("goals", "Goals", Icons.Default.Star),
                TabItem("learning", "Learn", Icons.Default.List),
                TabItem("sleep", "Sleep", Icons.Default.Notifications),
                TabItem("stats", "Stats", Icons.Default.Info),
                TabItem("finance", "Money", Icons.Default.ShoppingCart),
                TabItem("week", "Week", Icons.Default.Refresh)
            )

            tabItems.forEach { item ->
                val isSelected = selectedTab == item.id
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(item.id) }
                        .weight(1f)
                        .testTag("nav_${item.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(6.dp)
                                .background(InstaGradient, shape = RoundedCornerShape(100))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color.White else MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MutedText
                    )
                }
            }
        }
    }
}

data class TabItem(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)


// ============================================
// TODAY TAB SCREEN
// ============================================
@Composable
fun TodayTabScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val intents by viewModel.intents.collectAsStateWithLifecycle()

    var showAddHabit by rememberSaveable { mutableStateOf(false) }
    var showAddIntent by rememberSaveable { mutableStateOf(false) }
    var newHabitName by rememberSaveable { mutableStateOf("") }
    var newIntentName by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section: Daily Habits
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Habits Logs",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                IconButton(onClick = { showAddHabit = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = BrandAccent)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (habits.isEmpty()) {
                        Text(
                            text = "No habits. Add logs above!",
                            color = MutedText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        habits.forEach { habit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleHabit(habit) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (habit.isCompleted) InstaGradient else Brush.linearGradient(
                                                    listOf(Color(0xFF222222), Color(0xFF222222))
                                                )
                                            )
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (habit.isCompleted) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = habit.name,
                                        color = if (habit.isCompleted) MutedText else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (habit.streak > 0) {
                                        Text(
                                            text = "🔥 ${habit.streak}",
                                            color = InstaOrange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 8.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteHabit(habit.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Daily Intents
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Intent (Non-negotiables)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                IconButton(onClick = { showAddIntent = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Intent", tint = BrandAccent)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (intents.isEmpty()) {
                        Text(
                            text = "No daily intents set.",
                            color = MutedText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        intents.forEach { intent ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleIntent(intent) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (intent.isCompleted) InstaGradient else Brush.linearGradient(
                                                    listOf(Color(0xFF222222), Color(0xFF222222))
                                                )
                                            )
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (intent.isCompleted) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = intent.name,
                                        color = if (intent.isCompleted) MutedText else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { viewModel.deleteIntent(intent.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Modal: Add Habit Input Drawer / Dialog
    if (showAddHabit) {
        Dialog(onDismissRequest = { showAddHabit = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Track New Habit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = newHabitName,
                        onValueChange = { newHabitName = it },
                        placeholder = { Text("e.g. Mass gainer (12 PM)", color = MutedText, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("habit_input_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newHabitName.isNotBlank()) {
                                    viewModel.addHabit(newHabitName)
                                    newHabitName = ""
                                    showAddHabit = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f).testTag("save_habit_btn")
                        ) {
                            Text("Save", color = Color.White)
                        }
                        Button(
                            onClick = { showAddHabit = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Intent Dialog
    if (showAddIntent) {
        Dialog(onDismissRequest = { showAddIntent = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Add Daily Intent",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = newIntentName,
                        onValueChange = { newIntentName = it },
                        placeholder = { Text("e.g. Read Arabic vocabulary (10 words)", color = MutedText, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newIntentName.isNotBlank()) {
                                    viewModel.addIntent(newIntentName)
                                    newIntentName = ""
                                    showAddIntent = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = Color.White)
                        }
                        Button(
                            onClick = { showAddIntent = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


// ============================================
// GOALS TAB SCREEN
// ============================================
@Composable
fun GoalsTabScreen(viewModel: DashboardViewModel) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val pointLogs by viewModel.pointLogs.collectAsStateWithLifecycle()

    var showAddGoal by remember { mutableStateOf(false) }
    var showAddPointsId by remember { mutableStateOf<Long?>(null) }

    var goalName by remember { mutableStateOf("") }
    var goalWhy by remember { mutableStateOf("") }
    var goalStatus by remember { mutableStateOf("ACTIVE") }

    var logActivityName by remember { mutableStateOf("") }
    var logHours by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Core Point-System Goals",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddGoal = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = BrandAccent)
                }
            }
        }

        // Active Goals
        items(goals) { goal ->
            // Calculate progress cumulative logic
            val currentPoints = remember(pointLogs, goal.id) {
                pointLogs.filter { it.goalId == goal.id }.sumOf { it.hours.toDouble() }.toFloat() + goal.bonusPoints
            }
            val progressPercent = remember(currentPoints) {
                minOf(100f, currentPoints)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header (Title, pts counter)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = goal.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Why: ${goal.why}",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            GradientText(
                                text = "${String.format("%.1f", progressPercent)}%",
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = "${String.format("%.1f", currentPoints)} PTS",
                                color = InstaOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color(0xFF222222), shape = RoundedCornerShape(100))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((progressPercent / 100f).coerceIn(0f, 1f))
                                .background(InstaGradient, shape = RoundedCornerShape(100))
                        )
                    }

                    // Status Capsules & Action Links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (goal.status) {
                                "ACTIVE" -> InstaPurple.copy(alpha = 0.2f)
                                "NEXT" -> Color.DarkGray.copy(alpha = 0.4f)
                                else -> InstaOrange.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = goal.status,
                                color = when (goal.status) {
                                    "ACTIVE" -> InstaPurple
                                    "NEXT" -> Color.LightGray
                                    else -> InstaOrange
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showAddPointsId = goal.id },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+ Log Action", color = BrandAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { viewModel.deleteGoal(goal.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Inline Logs display drawer
                    val activeLogs = remember(pointLogs, goal.id) {
                        pointLogs.filter { it.goalId == goal.id }
                    }

                    if (activeLogs.isNotEmpty()) {
                        Divider(color = BorderHighlight, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Action Logs:", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        activeLogs.take(3).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "- ${log.activity}",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "+${log.hours} hrs/pts",
                                        color = InstaOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Log",
                                        tint = Color.Red.copy(0.4f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.deletePointLog(log.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Modal: Add Goal
    if (showAddGoal) {
        Dialog(onDismissRequest = { showAddGoal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Add Custom Goal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        placeholder = { Text("e.g. Start YT Gaming Channel", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = goalWhy,
                        onValueChange = { goalWhy = it },
                        placeholder = { Text("Why does this match your life purpose?", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Cycle status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ACTIVE", "NEXT", "SOMEDAY").forEach { targetStatus ->
                            val activeVal = goalStatus == targetStatus
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeVal) InstaGradient else Brush.linearGradient(
                                            listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { goalStatus = targetStatus }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(targetStatus, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (goalName.isNotBlank()) {
                                    viewModel.addGoal(goalName, goalWhy, goalStatus)
                                    goalName = ""
                                    goalWhy = ""
                                    showAddGoal = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = { showAddGoal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Modal: Log Points / Action
    if (showAddPointsId != null) {
        Dialog(onDismissRequest = { showAddPointsId = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Log Goal Progress Activity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = logActivityName,
                        onValueChange = { logActivityName = it },
                        placeholder = { Text("Activity (e.g. Studied Core 1 practice)", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = logHours,
                        onValueChange = { logHours = it },
                        placeholder = { Text("Hours spent (1hr = 1pt = 1% progress)", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val floatHours = logHours.toFloatOrNull() ?: 0.0f
                                if (logActivityName.isNotBlank() && floatHours > 0f) {
                                    viewModel.addPointsToGoal(showAddPointsId!!, logActivityName, floatHours)
                                    logActivityName = ""
                                    logHours = ""
                                    showAddPointsId = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Log Points")
                        }
                        Button(
                            onClick = { showAddPointsId = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}


// ============================================
// LEARNING TAB SCREEN (5 SUB-TABS)
// ============================================
@Composable
fun LearningTabScreen(viewModel: DashboardViewModel) {
    val learningItems by viewModel.learningItems.collectAsStateWithLifecycle()
    val wordsList by viewModel.words.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf("ALL") } // "ALL", "IT", "LANGUAGES", "VOCABULARY", "COURSES"

    var showAddItem by remember { mutableStateOf(false) }
    var showAddWord by remember { mutableStateOf(false) }

    // Forms
    var itemName by remember { mutableStateOf("") }
    var itemSubtext by remember { mutableStateOf("") }
    var itemCategory by remember { mutableStateOf("IT") }

    var wordOriginal by remember { mutableStateOf("") }
    var wordEnglish by remember { mutableStateOf("") }
    var wordLangCat by remember { mutableStateOf("ARABIC") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Sub tabs rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val subTabs = listOf("ALL", "IT", "ARABIC", "JAPANESE", "VOCABULARY", "COURSES")
            subTabs.forEach { tab ->
                val active = tab == activeSubTab
                Surface(
                    color = if (active) BrandAccent.copy(alpha = 0.2f) else LayerCard,
                    border = BorderStroke(1.dp, if (active) BrandAccent else BorderHighlight),
                    shape = RoundedCornerShape(100),
                    modifier = Modifier
                        .clickable { activeSubTab = tab }
                ) {
                    Text(
                        text = tab,
                        color = if (active) Color.White else MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Fast actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddItem = true },
                colors = ButtonDefaults.buttonColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Topic", modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Topic", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showAddWord = true },
                colors = ButtonDefaults.buttonColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Word", modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Word", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid lists of topics / words
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Render learning items
            val filteredLearning = learningItems.filter {
                activeSubTab == "ALL" || 
                it.category == activeSubTab ||
                (activeSubTab == "ARABIC" && it.category == "LANGUAGES" && it.name.contains("arabic", ignoreCase = true)) ||
                (activeSubTab == "JAPANESE" && it.category == "LANGUAGES" && it.name.contains("japanese", ignoreCase = true))
            }

            if (filteredLearning.isNotEmpty()) {
                item {
                    Text("Academic & IT Topics", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                items(filteredLearning) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LayerCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderHighlight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                Text(item.subtext, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${item.category} • ${item.status}",
                                    color = InstaOrange,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(onClick = { viewModel.deleteLearning(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.6f))
                            }
                        }
                    }
                }
            }

            // Render Words lists
            val filteredWords = wordsList.filter {
                activeSubTab == "ALL" ||
                        (activeSubTab == "ARABIC" && it.category == "ARABIC") ||
                        (activeSubTab == "JAPANESE" && it.category == "JAPANESE") ||
                        (activeSubTab == "VOCABULARY" && it.category == "ENGLISH")
            }

            if (filteredWords.isNotEmpty()) {
                val groupedWords = filteredWords.groupBy { it.category }
                val sortedCategories = listOf("ARABIC", "JAPANESE", "ENGLISH")
                
                sortedCategories.forEach { cat ->
                    val list = groupedWords[cat]
                    if (list != null && list.isNotEmpty()) {
                        val sectionTitle = when (cat) {
                            "ARABIC" -> "Arabic Dictionary Logs"
                            "JAPANESE" -> "Japanese Dictionary/Words List"
                            "ENGLISH" -> "Vocabulary & English Logs"
                            else -> "$cat Words"
                        }
                        
                        item {
                            Text(
                                text = sectionTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        items(list) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LayerCard),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, BorderHighlight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.word,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "Meaning: ${item.meaning}",
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Share",
                                                tint = InstaPurple,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "${item.category} (Tap voice symbol on WhatsApp)",
                                                color = MutedText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(onClick = { viewModel.deleteWord(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Topic
    if (showAddItem) {
        Dialog(onDismissRequest = { showAddItem = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Track Academic Topic", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = { Text("e.g. CompTIA A+ Core 2", color = MutedText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = itemSubtext,
                        onValueChange = { itemSubtext = it },
                        placeholder = { Text("Short focus note", color = MutedText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Pick Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("IT", "COURSES", "LANGUAGES").forEach { cat ->
                            val activeVal = itemCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeVal) InstaGradient else Brush.linearGradient(
                                            listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { itemCategory = cat }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (itemName.isNotBlank()) {
                                    viewModel.addLearning(itemName, itemSubtext, itemCategory, "ACTIVE")
                                    itemName = ""
                                    itemSubtext = ""
                                    showAddItem = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = { showAddItem = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Word Dialog
    if (showAddWord) {
        Dialog(onDismissRequest = { showAddWord = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Add Word Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = wordOriginal,
                        onValueChange = { wordOriginal = it },
                        placeholder = { Text("Word (e.g. Shukran)", color = MutedText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = wordEnglish,
                        onValueChange = { wordEnglish = it },
                        placeholder = { Text("English meaning", color = MutedText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Select language
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("ARABIC", "JAPANESE", "ENGLISH").forEach { lang ->
                            val activeVal = wordLangCat == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeVal) InstaGradient else Brush.linearGradient(
                                            listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { wordLangCat = lang }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lang, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (wordOriginal.isNotBlank() && wordEnglish.isNotBlank()) {
                                    viewModel.addWord(wordOriginal, wordEnglish, wordLangCat)
                                    wordOriginal = ""
                                    wordEnglish = ""
                                    showAddWord = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = { showAddWord = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }
}


// ============================================
// SLEEP TAB SCREEN
// ============================================
@Composable
fun SleepTabScreen(viewModel: DashboardViewModel) {
    val sleepLogs by viewModel.sleepLogs.collectAsStateWithLifecycle()

    var sleptHour by remember { mutableStateOf("01:00") }
    var wakeHour by remember { mutableStateOf("05:30") }

    // Analytics computation
    val averageSlept = remember(sleepLogs) {
        if (sleepLogs.isEmpty()) 0f else sleepLogs.sumOf { it.hoursSlept.toDouble() }.toFloat() / sleepLogs.size
    }
    val weeklyDeficit = remember(averageSlept) {
        maxOf(0f, (7.0f - averageSlept) * 7f)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Sleep Routine Calculator",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Stats boxes grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Average Slept Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderHighlight)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SAVG / NIGHT", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        GradientText(
                            text = "${String.format("%.1f", averageSlept)} hrs",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black)
                        )
                    }
                }

                // Target Slept Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderHighlight)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("MY TARGET", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "7.0 hrs",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Weekly Deficit Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderHighlight)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("WEEKLY DEBT", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${String.format("%.1f", weeklyDeficit)} hrs",
                            color = if (weeklyDeficit > 5f) Color.Red else InstaOrange,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Input Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Slept Last Evening", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sleptHour,
                            onValueChange = { sleptHour = it },
                            label = { Text("Slept At (e.g. 01:00)", fontSize = 10.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF222222),
                                unfocusedContainerColor = Color(0xFF1E1E1E)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = wakeHour,
                            onValueChange = { wakeHour = it },
                            label = { Text("Wake At (e.g. 05:30)", fontSize = 10.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF222222),
                                unfocusedContainerColor = Color(0xFF1E1E1E)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
                            viewModel.addSleep(currentDate, sleptHour, wakeHour)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log Routine Sleep", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Weekly grid (highlighting quality)
        item {
            Text("This Week's Routine Grid (Target: 6.5h+)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Pre-defined 7 days grid representation based on sleep logs
                    val calendar = Calendar.getInstance()
                    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")

                    for (i in 6 downTo 0) {
                        val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
                        val loggedObj = sleepLogs.find { it.dateString == dateStr }

                        val isComplete = loggedObj != null
                        val isGood = loggedObj != null && loggedObj.hoursSlept >= 6.5f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(dayNames.getOrElse(d.get(Calendar.DAY_OF_WEEK) - 1) { " " }, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isComplete) {
                                            if (isGood) InstaGradient else Brush.linearGradient(
                                                listOf(Color(0xFFFD1D1D), Color(0xFFFD1D1D))
                                            )
                                        } else {
                                            Brush.linearGradient(listOf(Color(0xFF222222), Color(0xFF222222)))
                                        }
                                    )
                                    .border(1.dp, BorderHighlight, shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isComplete) "${loggedObj!!.hoursSlept}h" else "-",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // List of past sleep entries
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sleep Schedule Logs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        items(sleepLogs.take(10)) { logEntry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(logEntry.dateString, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Bed: ${logEntry.sleptAt} • Wake: ${logEntry.wokeUp}",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${logEntry.hoursSlept} hrs",
                            color = if (logEntry.hoursSlept >= 6.5f) InstaOrange else Color.Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(end = 6.dp)
                        )

                        IconButton(onClick = { viewModel.deleteSleepLog(logEntry.dateString) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.6f))
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}


// ============================================
// STATS TAB SCREEN (HIGH-FIDELITY CUSTOM CANVAS CHARTS)
// ============================================
@Composable
fun StatsTabScreen(viewModel: DashboardViewModel) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val intents by viewModel.intents.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val pointLogs by viewModel.pointLogs.collectAsStateWithLifecycle()
    val wordsList by viewModel.words.collectAsStateWithLifecycle()
    val sleepLogs by viewModel.sleepLogs.collectAsStateWithLifecycle()

    // Calculate core aggregate card numbers
    val habitsDoneToday = habits.count { it.isCompleted }
    val habitsPct = if (habits.isEmpty()) 0 else (habitsDoneToday * 100) / habits.size

    val wordsCount = wordsList.size

    val avgGoalPct = remember(goals, pointLogs) {
        if (goals.isEmpty()) 0 else {
            val totalPct = goals.sumOf { goal ->
                val pts = pointLogs.filter { it.goalId == goal.id }.sumOf { it.hours.toDouble() } + goal.bonusPoints
                minOf(100.0, pts).toInt()
            }
            totalPct / goals.size
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Analytical Performance Insights", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        // Quick Stat Deck Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Habits completed
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    border = BorderStroke(1.dp, BorderHighlight),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("HABITS TODAY", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        GradientText("${habitsPct}%", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black))
                        Text("$habitsDoneToday / ${habits.size} done", color = Color.White, fontSize = 10.sp)
                    }
                }

                // Words learned
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    border = BorderStroke(1.dp, BorderHighlight),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("WORDS LEARNED", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        GradientText("$wordsCount", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black))
                        Text("AR/JP/Vocabulary", color = Color.White, fontSize = 10.sp)
                    }
                }

                // Average Goal
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    border = BorderStroke(1.dp, BorderHighlight),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AVG GOAL %", color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        GradientText("${avgGoalPct}%", style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black))
                        Text("Overall level", color = Color.White, fontSize = 10.sp)
                    }
                }
            }
        }

        // CHART 1: Sleep Hours Custom Canvas Bar Chart (Last 7 days)
        item {
            Text("Sleep Hours (Bar Chart)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val days = listOf("S", "M", "T", "W", "T", "F", "S")
                    val weeklySleep = remember(sleepLogs) {
                        val list = mutableListOf<Float>()
                        for (i in 6 downTo 0) {
                            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
                            val log = sleepLogs.find { it.dateString == dateStr }
                            list.add(log?.hoursSlept ?: 0f)
                        }
                        list
                    }

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    ) {
                        val barWidth = 22.dp.toPx()
                        val spacing = maxOf(2.dp.toPx(), (size.width - (barWidth * 7)) / 8)
                        val maxHours = 9f

                        for (i in 0 until 7) {
                            val valHours = weeklySleep.getOrElse(i) { 0f }
                            val heightOffset = maxOf(0.dp.toPx(), size.height - 20.dp.toPx())
                            val barHeight = (valHours / maxHours) * heightOffset
                            val x = spacing + i * (barWidth + spacing)
                            val y = maxOf(0f, size.height - barHeight - 16.dp.toPx())

                            // Draw shadow base
                            drawRoundRect(
                                color = Color(0x1AFFFFFF),
                                topLeft = Offset(x, 0f),
                                size = Size(barWidth, maxOf(0f, size.height - 16.dp.toPx())),
                                cornerRadius = CornerRadius(4.dp.toPx())
                            )

                            // Draw logged hours bar
                            if (valHours > 0) {
                                val brushColor = if (valHours >= 6.5f) {
                                    Brush.verticalGradient(listOf(InstaPurple, InstaRed))
                                } else {
                                    Brush.verticalGradient(listOf(InstaRed, InstaOrange))
                                }

                                drawRoundRect(
                                    brush = brushColor,
                                    topLeft = Offset(x, y),
                                    size = Size(barWidth, maxOf(0f, barHeight)),
                                    cornerRadius = CornerRadius(4.dp.toPx())
                                )
                            }
                        }
                    }

                    // Labels Row
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        days.forEach {
                            Text(it, color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp), textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }

        // CHART 2: Active Goal Progress Horizontal Bars
        item {
            Text("Active Goal Progress Rates", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val activeGoals = remember(goals) {
                        goals.filter { it.status == "ACTIVE" }.take(4)
                    }

                    if (activeGoals.isEmpty()) {
                        Text("No active goals to show statistics.", color = MutedText, fontSize = 11.sp)
                    } else {
                        activeGoals.forEach { goal ->
                            val currentPoints = remember(pointLogs, goal.id) {
                                pointLogs.filter { it.goalId == goal.id }.sumOf { it.hours.toDouble() }.toFloat() + goal.bonusPoints
                            }
                            val progressPercent = remember(currentPoints) {
                                minOf(100f, currentPoints)
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(goal.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text("${String.format("%.1f", progressPercent)}%", color = InstaOrange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .background(Color(0xFF222222), shape = RoundedCornerShape(100))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth((progressPercent / 100f).coerceIn(0f, 1f))
                                            .background(InstaGradient, shape = RoundedCornerShape(100))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // CHART 3: Habits completed Donut Ring
        item {
            Text("Habit Completion Breakdown", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 14.dp.toPx()
                            val pctFrac = habitsPct / 100f

                            // Background static circle
                            drawArc(
                                color = Color(0x12FFFFFF),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )

                            // Foreground sweep gradient arc
                            drawArc(
                                brush = InstaGradient,
                                startAngle = -90f,
                                sweepAngle = 360f * pctFrac,
                                useCenter = false,
                                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        GradientText(
                            text = "${habitsPct}%",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "$habitsDoneToday of ${habits.size} Habits done",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Daily checklist intent tracker is active. Check habits on Today Tab daily.",
                            color = MutedText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}


// ============================================
// WEEK TAB SCREEN
// ============================================
@Composable
fun WeekTabScreen(viewModel: DashboardViewModel) {
    val sleepLogs by viewModel.sleepLogs.collectAsStateWithLifecycle()
    val rEntity by viewModel.currentReflection.collectAsStateWithLifecycle()

    var textReflection by remember { mutableStateOf("") }
    var textIntention by remember { mutableStateOf("") }

    // Sync from database
    LaunchedEffect(rEntity) {
        if (rEntity != null) {
            textReflection = rEntity!!.reflection
            textIntention = rEntity!!.intention
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Weekly Deep Reflection",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Emoji display based on sleeping logs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Daily Sleeping Quality (Emoji row)", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")
                        for (i in 6 downTo 0) {
                            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
                            val log = sleepLogs.find { it.dateString == dateStr }

                            val emoji = when {
                                log == null -> "—"
                                log.hoursSlept >= 6.5f -> "😎"
                                log.hoursSlept >= 5.0f -> "😐"
                                else -> "😴"
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dayNames.getOrElse(d.get(Calendar.DAY_OF_WEEK) - 1) { " " }, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Reflection Note
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Weekly Feedback & Review", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = textReflection,
                        onValueChange = { textReflection = it },
                        placeholder = { Text("How was your weekly schedule, productivity, and sleep balance?", color = MutedText, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF222222),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Text("Next Week Action Steps", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = textIntention,
                        onValueChange = { textIntention = it },
                        placeholder = { Text("What single action items will you tackle next week?", color = MutedText, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF222222),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.saveReflection("2026-W22", textReflection, textIntention)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Reflections", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

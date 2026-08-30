package android.example.myapplication.ui.stats

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.example.myapplication.ui.theme.*
import data.TaskDatabase
import data.TaskRepository
import android.content.Intent
import android.example.myapplication.MainActivity
import android.example.myapplication.ui.calendar.CalendarActivity

class StatisticsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModel = StatisticsViewModel(repository)

        setContent {
            MyApplicationTheme {
                StatisticsScreen(viewModel) { index ->
                    when(index) {
                        0 -> startActivity(Intent(this, MainActivity::class.java))
                        1 -> startActivity(Intent(this, CalendarActivity::class.java))
                    }
                    if (index != 2) finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(viewModel: StatisticsViewModel, onNavClick: (Int) -> Unit) {
    BackHandler {
        onNavClick(0)
    }
    val stats by viewModel.stats.collectAsState(initial = TaskStats(0, 0, 0, 0f))
    var selectedPeriod by remember { mutableStateOf("Week") }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                title = { Text("Statistics", style = Typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = { onNavClick(0) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavClick(0) },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { onNavClick(1) },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Calendar") }
                )
                NavigationBarItem(
                    selected = true,
                    onClick = { },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = null) }, // Stats icon placeholder
                    label = { Text("Stats") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGradientEnd,
                        selectedTextColor = BrandGradientEnd,
                        indicatorColor = BrandGradientStart.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    label = { Text("More") }
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                // Period Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
                        .padding(4.dp)
                ) {
                    listOf("Week", "Month", "All Time").forEach { period ->
                        val isSelected = selectedPeriod == period
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(if (isSelected) BrandGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)), RoundedCornerShape(20.dp))
                                .shadow(if (isSelected) 4.dp else 0.dp, RoundedCornerShape(20.dp))
                                .clickable { selectedPeriod = period },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = period,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            item {
                // Tasks Completed Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Tasks Completed", style = Typography.titleMedium, color = TextPrimary)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = stats.completedTasks.toString(),
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = " of ${stats.totalTasks} tasks",
                                modifier = Modifier.padding(bottom = 12.dp, start = 8.dp),
                                color = TextSecondary,
                                style = Typography.bodyMedium
                            )
                        }
                        // Simple Bar Chart Placeholder
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .padding(top = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            val heights = listOf(0.4f, 0.2f, 0.6f, 0.3f, 0.7f, 0.9f, 0.5f)
                            heights.forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(12.dp)
                                        .fillMaxHeight(h)
                                        .background(BrandGradient, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun").forEach {
                                Text(it, fontSize = 10.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }

            item {
                // Summary Grid
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Completion Rate",
                        value = "${(stats.completionRate * 100).toInt()}%",
                        color = AccentBlue,
                        showProgress = true,
                        progress = stats.completionRate
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Study Streak",
                        value = "7 days",
                        color = Color(0xFFFFA500), // Orange
                        showIcon = true
                    )
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Total Tasks",
                        value = stats.totalTasks.toString(),
                        color = BrandGradientStart
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        label = "Pending Tasks",
                        value = stats.pendingTasks.toString(),
                        color = PriorityHighText
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    modifier: Modifier,
    label: String,
    value: String,
    color: Color,
    showProgress: Boolean = false,
    progress: Float = 0f,
    showIcon: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, style = Typography.bodySmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (showProgress) {
                    Spacer(modifier = Modifier.width(12.dp))
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.size(24.dp),
                        color = color,
                        strokeWidth = 3.dp,
                        trackColor = color.copy(alpha = 0.1f)
                    )
                }
                if (showIcon) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🔥", fontSize = 20.sp)
                }
            }
        }
    }
}

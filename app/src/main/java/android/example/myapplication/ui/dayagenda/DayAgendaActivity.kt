package android.example.myapplication.ui.dayagenda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.TaskDatabase
import data.TaskRepository
import android.example.myapplication.ui.theme.*
import android.example.myapplication.StudyTask
import android.example.myapplication.TaskCard
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DayAgendaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val selectedDate = intent.getStringExtra("selected_date") ?: ""
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModel = DayAgendaViewModel(repository)

        setContent {
            MyApplicationTheme {
                DayAgendaScreen(selectedDate, viewModel) {
                    finish()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayAgendaScreen(date: String, viewModel: DayAgendaViewModel, onBack: () -> Unit) {
    val entities by viewModel.getTasksByDate(date).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Calendar as this is reached via calendar

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                title = { Text(date, style = Typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Back") // Placeholder for back
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { 
                        selectedTab = 0
                        context.startActivity(android.content.Intent(context, android.example.myapplication.MainActivity::class.java))
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { 
                        selectedTab = 1
                        onBack() // Go back to calendar
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = null) },
                    label = { Text("Calendar") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGradientEnd,
                        selectedTextColor = BrandGradientEnd,
                        indicatorColor = BrandGradientStart.copy(alpha = 0.1f)
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { 
                        selectedTab = 2
                        context.startActivity(android.content.Intent(context, android.example.myapplication.ui.stats.StatisticsActivity::class.java))
                    },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    label = { Text("Stats") }
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { },
                    icon = { Icon(Icons.Default.MoreVert, contentDescription = null) },
                    label = { Text("More") }
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    style = Typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${entities.size} tasks",
                    style = Typography.bodySmall,
                    color = TextSecondary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            if (entities.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No tasks for this day", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(entities) { entity ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            // Time and Dot
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.width(60.dp)
                            ) {
                                Text(
                                    text = entity.time,
                                    style = Typography.bodySmall,
                                    color = AccentBlue,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(AccentBlue, CircleShape)
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(8.dp))
                            
                            val studyTask = StudyTask(
                                id = entity.id,
                                title = entity.title,
                                subject = entity.subject,
                                date = entity.date,
                                time = entity.time,
                                priority = entity.priority,
                                repeat = entity.repeat,
                                completed = entity.completed
                            )
                            
                            TaskCard(
                                task = studyTask,
                                onDelete = {
                                    scope.launch { viewModel.deleteTask(entity) }
                                },
                                onToggleCompleted = {
                                    scope.launch { viewModel.updateTask(entity.copy(completed = !entity.completed)) }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

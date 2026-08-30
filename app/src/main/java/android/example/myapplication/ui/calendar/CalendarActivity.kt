package android.example.myapplication.ui.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.TaskDatabase
import data.TaskRepository
import data.TaskEntity
import android.example.myapplication.ui.theme.MyApplicationTheme
import android.example.myapplication.ui.dayagenda.DayAgendaActivity
import android.content.Intent
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.daysOfWeek
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.*

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import android.example.myapplication.ui.theme.*
import android.example.myapplication.StudyTask
import android.example.myapplication.TaskCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModel = CalendarViewModel(repository)

        setContent {
            MyApplicationTheme {
                CalendarScreen(viewModel, onBack = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onBack: () -> Unit) {
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val daysOfWeek = remember { daysOfWeek() }
    val context = LocalContext.current

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )
    
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(1) }

    Scaffold(
        containerColor = AppBackground,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBackground),
                title = { Text("Calendar", style = Typography.titleLarge, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
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
                        onBack()
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { },
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
                        val intent = android.content.Intent(context, android.example.myapplication.ui.stats.StatisticsActivity::class.java)
                        context.startActivity(intent)
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
        ) {
            // Calendar Card
            Card(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Month Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val monthName = state.firstVisibleMonth.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        val year = state.firstVisibleMonth.yearMonth.year
                        
                        IconButton(onClick = {
                            scope.launch {
                                state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.minusMonths(1))
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous")
                        }
                        
                        Text(
                            text = "$monthName $year",
                            style = Typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(onClick = {
                            scope.launch {
                                state.animateScrollToMonth(state.firstVisibleMonth.yearMonth.plusMonths(1))
                            }
                        }) {
                            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Weekday Initials
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (dayOfWeek in daysOfWeek) {
                            Text(
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(1),
                                style = Typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalCalendar(
                        state = state,
                        dayContent = { day ->
                            Day(
                                day = day, 
                                isSelected = selectedDate == day.date,
                                tasks = tasks
                            ) { clickedDay ->
                                selectedDate = clickedDay.date
                            }
                        }
                    )
                }
            }
            
            // Day Agenda List
            val selectedDateString = "${selectedDate.dayOfMonth}/${selectedDate.monthValue}/${selectedDate.year}"
            val dayTasks = tasks.filter { it.date == selectedDateString }
            
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d")
                    Text(
                        text = selectedDate.format(formatter),
                        style = Typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${dayTasks.size} tasks",
                        style = Typography.bodySmall,
                        color = TextSecondary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (dayTasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No tasks for this day", color = TextSecondary)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(dayTasks) { entity ->
                            TimelineTaskItem(entity, viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun Day(
    day: CalendarDay, 
    isSelected: Boolean,
    tasks: List<TaskEntity>, 
    onClick: (CalendarDay) -> Unit
) {
    val dateString = "${day.date.dayOfMonth}/${day.date.monthValue}/${day.date.year}"
    val hasTasks = tasks.any { it.date == dateString }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(if (isSelected) AccentBlue else Color.Transparent)
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = when {
                    isSelected -> Color.White
                    day.position == DayPosition.MonthDate -> TextPrimary
                    else -> TextSecondary.copy(alpha = 0.3f)
                },
                style = Typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (hasTasks && day.position == DayPosition.MonthDate) {
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(4.dp)
                        .background(if (isSelected) Color.White else AccentBlue, shape = CircleShape)
                )
            }
        }
    }
}

@Composable
fun TimelineTaskItem(entity: TaskEntity, viewModel: CalendarViewModel) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth()) {
        // Time and vertical line
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
            // Vertical line could be added here if needed to perfectly match image
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Task Card
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
                viewModel.deleteTask(entity)
            },
            onToggleCompleted = { 
                viewModel.updateTask(entity.copy(completed = !entity.completed))
            }
        )
    }
}

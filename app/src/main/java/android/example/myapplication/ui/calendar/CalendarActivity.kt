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

class CalendarActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = TaskDatabase.getDatabase(this)
        val repository = TaskRepository(database.taskDao())
        val viewModel = CalendarViewModel(repository)

        setContent {
            MyApplicationTheme {
                CalendarScreen(viewModel) { date ->
                    val intent = Intent(this, DayAgendaActivity::class.java).apply {
                        putExtra("selected_date", date)
                    }
                    startActivity(intent)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel, onDayClick: (String) -> Unit) {
    val tasks by viewModel.allTasks.collectAsState(initial = emptyList())
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val daysOfWeek = remember { daysOfWeek() }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calendar") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            HorizontalCalendar(
                state = state,
                dayContent = { day ->
                    Day(day, tasks) {
                        val dateString = "${day.date.dayOfMonth}/${day.date.monthValue}/${day.date.year}"
                        onDayClick(dateString)
                    }
                },
                monthHeader = { month ->
                    val daysOfWeek = daysOfWeek()
                    Column {
                        Text(
                            modifier = Modifier.fillMaxWidth(),
                            text = month.yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + month.yearMonth.year,
                            fontSize = 20.sp,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold
                        )
                        Row(modifier = Modifier.fillMaxWidth()) {
                            for (dayOfWeek in daysOfWeek) {
                                Text(
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun Day(day: CalendarDay, tasks: List<TaskEntity>, onClick: (CalendarDay) -> Unit) {
    val hasTasks = tasks.any { it.date == "${day.date.dayOfMonth}/${day.date.monthValue}/${day.date.year}" }
    
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onClick(day) }
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                color = if (day.position == DayPosition.MonthDate) Color.Black else Color.Gray
            )
            if (hasTasks && day.position == DayPosition.MonthDate) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = androidx.compose.foundation.shape.CircleShape)
                )
            }
        }
    }
}